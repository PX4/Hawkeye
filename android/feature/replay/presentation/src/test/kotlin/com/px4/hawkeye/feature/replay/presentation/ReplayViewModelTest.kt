package com.px4.hawkeye.feature.replay.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import com.px4.hawkeye.core.domain.Result
import com.px4.hawkeye.feature.replay.domain.ReplayError
import com.px4.hawkeye.feature.replay.domain.UlogPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReplayViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var fake: FakeUlogInboxDataSource
    private lateinit var viewModel: ReplayViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        fake = FakeUlogInboxDataSource()
        viewModel = ReplayViewModel(fake)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `OnAppStarted on plain cold launch clears inbox and shows NoFileLoaded`() = runTest {
        viewModel.onAction(ReplayAction.OnAppStarted(fromFreshIngest = false))

        assertThat(fake.clearInboxCount).isEqualTo(1)
        assertThat(viewModel.state.value.dialog).isEqualTo(ReplayDialog.NoFileLoaded)
    }

    @Test
    fun `OnAppStarted from fresh ingest leaves inbox alone and shows no dialog`() = runTest {
        viewModel.onAction(ReplayAction.OnAppStarted(fromFreshIngest = true))

        assertThat(fake.clearInboxCount).isEqualTo(0)
        assertThat(viewModel.state.value.dialog).isNull()
    }

    @Test
    fun `OnIntentReceived resolves preview and shows ConfirmOpen`() = runTest {
        fake.previewResult = Result.Success(
            UlogPreview(displayName = "flight_2025_05_21.ulg", source = "com.example.docs")
        )

        viewModel.onAction(ReplayAction.OnIntentReceived("content://foo/bar"))

        assertThat(viewModel.state.value.dialog).isEqualTo(
            ReplayDialog.ConfirmOpen(
                displayName = "flight_2025_05_21.ulg",
                source = "com.example.docs"
            )
        )
    }

    @Test
    fun `second OnIntentReceived replaces the first ConfirmOpen dialog`() = runTest {
        fake.previewResult = Result.Success(UlogPreview("first.ulg", "auth-1"))
        viewModel.onAction(ReplayAction.OnIntentReceived("content://1"))

        fake.previewResult = Result.Success(UlogPreview("second.ulg", "auth-2"))
        viewModel.onAction(ReplayAction.OnIntentReceived("content://2"))

        assertThat(viewModel.state.value.dialog).isEqualTo(
            ReplayDialog.ConfirmOpen(displayName = "second.ulg", source = "auth-2")
        )
    }

    @Test
    fun `OnConfirmOpen triggers ingest, emits success toast, clears dialog`() = runTest {
        viewModel.onAction(ReplayAction.OnIntentReceived("content://x"))

        viewModel.events.test {
            viewModel.onAction(ReplayAction.OnConfirmOpen)
            assertThat(awaitItem()).isInstanceOf(ReplayEvent.ShowToast::class)
        }
        assertThat(fake.ingestedUris).isEqualTo(listOf("content://x"))
        assertThat(viewModel.state.value.dialog).isNull()
    }

    @Test
    fun `OnConfirmOpen with ingest failure emits error toast and clears dialog`() = runTest {
        fake.ingestResult = Result.Error(ReplayError.WRITE_FAILED)
        viewModel.onAction(ReplayAction.OnIntentReceived("content://x"))

        viewModel.events.test {
            viewModel.onAction(ReplayAction.OnConfirmOpen)
            assertThat(awaitItem()).isInstanceOf(ReplayEvent.ShowToast::class)
        }
        assertThat(viewModel.state.value.dialog).isNull()
    }

    @Test
    fun `OnDismissDialog clears state and does not ingest`() = runTest {
        viewModel.onAction(ReplayAction.OnIntentReceived("content://x"))

        viewModel.onAction(ReplayAction.OnDismissDialog)

        assertThat(viewModel.state.value.dialog).isNull()
        assertThat(fake.ingestedUris).isEqualTo(emptyList())
    }

    @Test
    fun `preview failure emits error toast and leaves dialog null`() = runTest {
        fake.previewResult = Result.Error(ReplayError.OPEN_FAILED)

        viewModel.events.test {
            viewModel.onAction(ReplayAction.OnIntentReceived("content://bad"))
            assertThat(awaitItem()).isInstanceOf(ReplayEvent.ShowToast::class)
        }
        assertThat(viewModel.state.value.dialog).isNull()
        assertThat(fake.ingestedUris).isEqualTo(emptyList())
    }
}
