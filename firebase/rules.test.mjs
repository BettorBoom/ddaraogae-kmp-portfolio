import { test, before, after, beforeEach } from 'node:test';
import { readFile } from 'node:fs/promises';
import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing';
import { doc, setDoc, getDoc, updateDoc, deleteDoc, collection, getDocs } from 'firebase/firestore';
import { ref, uploadBytes, getBytes } from 'firebase/storage';

let env;
const account = (uid, lifecycle = 'active') => ({ uid, lifecycle, schemaVersion: 1 });
const db = (uid) => uid ? env.authenticatedContext(uid).firestore() : env.unauthenticatedContext().firestore();
const user = (client, uid = 'alice') => doc(client, 'users', uid);
before(async () => {
  env = await initializeTestEnvironment({ projectId: 'demo-ddaraogae-kmp',
    firestore: { host: '127.0.0.1', port: 8080, rules: await readFile('firestore.rules', 'utf8') },
    storage: { host: '127.0.0.1', port: 9199, rules: await readFile('storage.rules', 'utf8') } });
});
after(async () => { await env?.cleanup(); });
beforeEach(async () => { await env.clearFirestore(); });
const seed = async (lifecycle = 'active') => env.withSecurityRulesDisabled(async (context) => {
  await setDoc(user(context.firestore()), account('alice', lifecycle));
});

test('owner creates and reads exact account schema', async () => {
  const client = db('alice');
  await assertSucceeds(setDoc(user(client), account('alice')));
  await assertSucceeds(getDoc(user(client)));
});
test('unauthenticated read and create are denied', async () => {
  await seed();
  await assertFails(getDoc(user(db())));
  await assertFails(setDoc(user(db(), 'guest'), account('guest')));
});
test('another UID cannot read, overwrite or delete', async () => {
  await seed(); const path = user(db('bob'));
  await assertFails(getDoc(path));
  await assertFails(setDoc(path, account('bob')));
  await assertFails(deleteDoc(path));
});
test('document UID spoof is denied', async () => {
  await assertFails(setDoc(user(db('alice')), account('bob')));
});
test('extra fields and missing fields are denied', async () => {
  await assertFails(setDoc(user(db('alice')), { ...account('alice'), admin: true }));
  await assertFails(setDoc(user(db('alice')), { uid: 'alice', lifecycle: 'active' }));
});
test('unknown schema version and invalid lifecycle are denied', async () => {
  await assertFails(setDoc(user(db('alice')), { ...account('alice'), schemaVersion: 2 }));
  await assertFails(setDoc(user(db('alice')), account('alice', 'unknown')));
});
test('owner can mark active account deleting', async () => {
  await seed(); await assertSucceeds(updateDoc(user(db('alice')), { lifecycle: 'deleting' }));
});
test('deleting account cannot be reactivated by late write', async () => {
  await seed('deleting');
  await assertFails(updateDoc(user(db('alice')), { lifecycle: 'active' }));
  await assertFails(setDoc(user(db('alice')), account('alice')));
});
test('deleting cannot be initial state and client cannot remove account', async () => {
  await assertFails(setDoc(user(db('alice')), account('alice', 'deleting')));
  await seed(); await assertFails(deleteDoc(user(db('alice'))));
});
test('global user enumeration is denied', async () => {
  await seed(); await assertFails(getDocs(collection(db('alice'), 'users')));
});
test('unimplemented child and root schemas stay denied for owner', async () => {
  await seed();
  await assertFails(setDoc(doc(db('alice'), 'users/alice/walks/walk1'), { uid: 'alice' }));
  await assertFails(setDoc(doc(db('alice'), 'anything/document'), { uid: 'alice' }));
});
test('storage upload denied even for matching owner until object schema exists', async () => {
  for (const uid of ['alice', 'bob', null]) {
    const context = uid ? env.authenticatedContext(uid) : env.unauthenticatedContext();
    await assertFails(uploadBytes(ref(context.storage(), 'users/alice/dogs/dog1.jpg'), new Uint8Array([1])));
  }
});
test('storage object is not publicly readable', async () => {
  await env.withSecurityRulesDisabled(async context => {
    await uploadBytes(ref(context.storage(), 'users/alice/dogs/dog1.jpg'), new Uint8Array([1]));
  });
  await assertFails(getBytes(ref(env.unauthenticatedContext().storage(), 'users/alice/dogs/dog1.jpg')));
});
