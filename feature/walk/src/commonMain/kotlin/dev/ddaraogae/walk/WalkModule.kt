package dev.ddaraogae.walk

import org.koin.core.qualifier.named
import org.koin.dsl.module

val walkModule = module {
    scope(named("user")) { factory { WalkViewModel(get()) } }
}
