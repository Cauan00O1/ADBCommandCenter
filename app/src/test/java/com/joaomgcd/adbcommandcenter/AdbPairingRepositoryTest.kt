package com.joaomgcd.adbcommandcenter.discovery

import android.util.Log
import com.joaomgcd.adb.AdbDiscoveryManager
import com.joaomgcd.adb.AdbService
import com.joaomgcd.adbcommandcenter.adb.common.domain.AdbRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.InetAddress

@OptIn(ExperimentalCoroutinesApi::class)
class AdbPairingRepositoryTest {

    private lateinit var adbDiscoveryManager: AdbDiscoveryManager
    private lateinit var adbRepository: AdbRepository

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val isSearchingFlow = MutableStateFlow(false)
    private val discoveredPairingServiceFlow = MutableStateFlow<AdbService?>(null)
    private val discoveredConnectServiceFlow = MutableSharedFlow<AdbService?>()

    private val connectServiceUpdatesFlow = MutableSharedFlow<AdbService?>(
        replay = 0,
        extraBufferCapacity = 100
    )

    @Before
    fun setup() {

        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0


        adbDiscoveryManager = mockk(relaxed = true)
        adbRepository = mockk(relaxed = true)

        every { adbDiscoveryManager.isSearching } returns isSearchingFlow
        every { adbDiscoveryManager.discoveredPairingService } returns discoveredPairingServiceFlow
        every { adbDiscoveryManager.discoveredConnectService } returns discoveredConnectServiceFlow
        every { adbDiscoveryManager.connectServiceUpdates } returns connectServiceUpdatesFlow
    }

    @Test
    fun `test stale port update does NOT clear paired state`() = testScope.runTest {
        val repository = AdbPairingRepositoryImpl(adbDiscoveryManager, adbRepository, this.backgroundScope)

        val hostIp = "192.168.1.51"
        val validService = createMockService(hostIp, 5555)
        val staleService = createMockService(hostIp, 4444)

        coEvery { adbRepository.isPaired(hostIp, 5555) } returns true
        coEvery { adbRepository.isPaired(hostIp, 4444) } returns false

        repository.startMonitoring()


        connectServiceUpdatesFlow.emit(validService)
        advanceUntilIdle()

        val state1 = repository.state.first()
        assertTrue(state1 is PairingState.Paired)
        assertEquals(5555, (state1 as PairingState.Paired).connectionService.port)


        connectServiceUpdatesFlow.emit(staleService)
        advanceUntilIdle()

        val state2 = repository.state.first()
        assertTrue("State should remain Paired despite stale update", state2 is PairingState.Paired)
        assertEquals("Should preserve valid port", 5555, (state2 as PairingState.Paired).connectionService.port)
    }

    private fun TestScope.createRepository(): AdbPairingRepositoryImpl {
        val repo = AdbPairingRepositoryImpl(
            adbDiscoveryManager,
            adbRepository,
            this.backgroundScope
        )
        this.backgroundScope.launch(UnconfinedTestDispatcher(this.testScheduler)) {
            repo.state.collect()
        }
        return repo
    }

    @Test
    fun `test real port change updates paired state`() = testScope.runTest {
        val repository = AdbPairingRepositoryImpl(adbDiscoveryManager, adbRepository, this.backgroundScope)

        val hostIp = "192.168.1.50"
        val oldService = createMockService(hostIp, 5555)
        val newService = createMockService(hostIp, 6666)

        coEvery { adbRepository.isPaired(hostIp, 5555) } returns true
        coEvery { adbRepository.isPaired(hostIp, 6666) } returns true

        repository.startMonitoring()


        connectServiceUpdatesFlow.emit(oldService)
        advanceUntilIdle()


        connectServiceUpdatesFlow.emit(newService)
        advanceUntilIdle()

        val state = repository.state.first()
        assertTrue(state is PairingState.Paired)
        assertEquals(6666, (state as PairingState.Paired).connectionService.port)
    }

    @Test
    fun `test initial connection successful`() = testScope.runTest {
        val repository = createRepository()
        val service = createMockService("10.0.0.1", 5555)

        coEvery { adbRepository.isPaired("10.0.0.1", 5555) } returns true

        repository.startMonitoring()

        connectServiceUpdatesFlow.emit(service)
        advanceUntilIdle()

        val state = repository.state.value
        assertTrue(state is PairingState.Paired)
    }

    @Test
    fun `test initial connection ignored if not paired`() = testScope.runTest {
        val repository = createRepository()
        val service = createMockService("10.0.0.1", 5555)

        coEvery { adbRepository.isPaired("10.0.0.1", 5555) } returns false

        repository.startMonitoring()

        connectServiceUpdatesFlow.emit(service)
        advanceUntilIdle()

        assertTrue(repository.state.value is PairingState.Idle)
    }


    @Test
    fun `test connection lost (null service)`() = testScope.runTest {
        val repository = createRepository()
        val service = createMockService("10.0.0.1", 5555)
        coEvery { adbRepository.isPaired(any(), any()) } returns true

        repository.startMonitoring()

        connectServiceUpdatesFlow.emit(service)
        advanceUntilIdle()
        assertTrue(repository.state.value is PairingState.Paired)

        connectServiceUpdatesFlow.emit(null)
        advanceUntilIdle()

        assertTrue("Should wait before disconnecting", repository.state.value is PairingState.Paired)

        testScope.advanceTimeBy(1001)
        advanceUntilIdle()

        assertTrue("State should revert after grace period", repository.state.value !is PairingState.Paired)
    }

    @Test
    fun `test switch to new valid host`() = testScope.runTest {
        val repository = createRepository()
        val hostA = "192.168.1.50"
        val hostB = "192.168.1.99"

        coEvery { adbRepository.isPaired(hostA, 5555) } returns true
        coEvery { adbRepository.isPaired(hostB, 5555) } returns true

        repository.startMonitoring()


        connectServiceUpdatesFlow.emit(createMockService(hostA, 5555))
        advanceUntilIdle()


        connectServiceUpdatesFlow.emit(createMockService(hostB, 5555))
        advanceUntilIdle()

        val state = repository.state.value as PairingState.Paired
        assertEquals("Should switch to Host B", hostB, state.connectionService.host.hostAddress)
    }

    @Test
    fun `test switch to invalid host clears state`() = testScope.runTest {
        val repository = createRepository()
        val hostA = "192.168.1.50"
        val hostB = "192.168.1.99"

        coEvery { adbRepository.isPaired(hostA, 5555) } returns true
        coEvery { adbRepository.isPaired(hostB, 5555) } returns false

        repository.startMonitoring()


        connectServiceUpdatesFlow.emit(createMockService(hostA, 5555))
        advanceUntilIdle()
        assertTrue(repository.state.value is PairingState.Paired)


        connectServiceUpdatesFlow.emit(createMockService(hostB, 5555))
        advanceUntilIdle()

        assertTrue("Should drop Paired state", repository.state.value !is PairingState.Paired)
    }

    @Test
    fun `test submitting pairing code updates state immediately`() = testScope.runTest {
        val repository = createRepository()
        val ip = "10.0.0.5"
        val service = createMockService(ip, 5555)

        coEvery { adbRepository.isPaired(ip, 5555) } returns false
        coEvery { adbRepository.pairDevice(ip, 5555, "123456") } returns Result.success(Unit)

        repository.startMonitoring()

        connectServiceUpdatesFlow.emit(service)
        advanceUntilIdle()

        assertTrue(repository.state.value !is PairingState.Paired)

        repository.submitPairingCode(ip, 5555, "123456")
        advanceUntilIdle()

        val state = repository.state.value
        assertTrue("Manual pairing should force update state", state is PairingState.Paired)
        assertEquals(ip, (state as PairingState.Paired).connectionService.host.hostAddress)
    }

    @Test
    fun `test Paired state overrides Scanning state`() = testScope.runTest {
        val repository = createRepository()
        val service = createMockService("10.0.0.1", 5555)
        coEvery { adbRepository.isPaired(any(), any()) } returns true

        repository.startMonitoring()

        isSearchingFlow.value = true
        advanceUntilIdle()
        assertTrue(repository.state.value is PairingState.Scanning)


        connectServiceUpdatesFlow.emit(service)
        advanceUntilIdle()

        assertTrue("Paired should override Scanning", repository.state.value is PairingState.Paired)
    }

    @Test
    fun `test PairingServiceFound state`() = testScope.runTest {
        val repository = createRepository()
        val pairingService = createMockService("10.0.0.1", 40000)

        repository.startMonitoring()


        discoveredPairingServiceFlow.value = pairingService
        advanceUntilIdle()

        val state = repository.state.value
        assertTrue(state is PairingState.PairingServiceFound)
        assertEquals(pairingService, (state as PairingState.PairingServiceFound).service)
    }
    @Test
    fun `test port storm finds the single valid port`() = testScope.runTest {
        val repository = createRepository()
        val host = "192.168.1.50"
        val validPort = 5555
        val validService = createMockService(host, validPort)


        coEvery { adbRepository.isPaired(host, validPort) } returns true
        coEvery { adbRepository.isPaired(host, neq(validPort)) } returns false

        repository.startMonitoring()


        repeat(20) { i ->
            connectServiceUpdatesFlow.emit(createMockService(host, 30000 + i))
        }


        connectServiceUpdatesFlow.emit(validService)


        repeat(20) { i ->
            connectServiceUpdatesFlow.emit(createMockService(host, 40000 + i))
        }

        advanceUntilIdle()


        val state = repository.state.value
        assertTrue("State should be Paired", state is PairingState.Paired)
        assertEquals("Should match the valid port", validPort, (state as PairingState.Paired).connectionService.port)
    }

    @Test
    fun `test noise on same host does not disconnect valid pairing`() = testScope.runTest {
        val repository = createRepository()
        val host = "192.168.1.50"
        val validService = createMockService(host, 5555)
        val noiseService = createMockService(host, 6666)

        coEvery { adbRepository.isPaired(host, 5555) } returns true
        coEvery { adbRepository.isPaired(host, 6666) } returns false

        repository.startMonitoring()


        connectServiceUpdatesFlow.emit(validService)
        advanceUntilIdle()


        assertTrue(repository.state.value is PairingState.Paired)


        connectServiceUpdatesFlow.emit(noiseService)
        advanceUntilIdle()



        val state = repository.state.value
        assertTrue("Should remain Paired despite noise", state is PairingState.Paired)
        assertEquals("Should preserve connection to 5555", 5555, (state as PairingState.Paired).connectionService.port)
    }

    @Test
    fun `test invalid port on DIFFERENT host causes disconnect`() = testScope.runTest {
        val repository = createRepository()
        val hostA = "192.168.1.50"
        val hostB = "192.168.1.99"

        val validServiceA = createMockService(hostA, 5555)
        val invalidServiceB = createMockService(hostB, 6666)

        coEvery { adbRepository.isPaired(hostA, 5555) } returns true
        coEvery { adbRepository.isPaired(hostB, 6666) } returns false

        repository.startMonitoring()


        connectServiceUpdatesFlow.emit(validServiceA)
        advanceUntilIdle()
        assertTrue(repository.state.value is PairingState.Paired)


        connectServiceUpdatesFlow.emit(invalidServiceB)
        advanceUntilIdle()



        val state = repository.state.value
        assertTrue("Should disconnect if different host appears unverified", state !is PairingState.Paired)
    }
    @Test
    fun `test scanning mode persists when invalid ports are found`() = testScope.runTest {
        val repository = createRepository()
        val invalidService = createMockService("192.168.1.55", 5555)


        coEvery { adbRepository.isPaired("192.168.1.55", 5555) } returns false

        repository.startMonitoring()


        isSearchingFlow.value = true
        advanceUntilIdle()
        assertTrue("Should be Scanning initially", repository.state.value is PairingState.Scanning)


        connectServiceUpdatesFlow.emit(invalidService)
        advanceUntilIdle()


        val state = repository.state.value
        assertTrue("Should remain Scanning when invalid port found", state is PairingState.Scanning)
        assertTrue("Should not be Paired", state !is PairingState.Paired)
    }




    private fun createMockService(ip: String, port: Int): AdbService {
        val mockHost = mockk<InetAddress>()
        every { mockHost.hostAddress } returns ip
        return AdbService(mockHost, port)
    }
}