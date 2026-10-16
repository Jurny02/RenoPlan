package dev.jurczyk.renoplan.di

import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatform
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull

class KoinModulesTest {
    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun appModulesStartWithoutErrors() {
        initKoin()

        assertNotNull(KoinPlatform.getKoinOrNull())
    }
}
