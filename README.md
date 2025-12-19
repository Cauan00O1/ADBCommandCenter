# ADB Command Center

ADB Command Center is a modular Android application designed to execute ADB commands locally via Wireless Debugging. It eliminates the need for a tethered PC by leveraging the local ADB protocol to manage permissions, install APKs, and run shell commands directly on the device.

[Check out a video demo of the app here](https://youtu.be/PSNmHfQ8BIY).

## Architecture & Tech Stack

The project is built using **Clean Architecture** principles and modern Android development practices:
*   **UI**: Jetpack Compose with Type-Safe Navigation.
*   **Dependency Injection**: Hilt.
*   **Asynchronous Logic**: Kotlin Coroutines and Flow.
*   **ADB Core**: Custom implementation of the ADB protocol for local execution.
*   **Automation**: First-class Tasker plugin support via `taskerpluginlibrary`.

---

## How to Add a New ADB Screen

To add a new feature to the main application grid:

### 1. Define the Route
Add a `@Serializable` destination to [Screen.kt](https://github.com/joaomgcd/ADBCommandCenter/blob/main/app/src/main/java/com/joaomgcd/adbcommandcenter/main/navigation/Screen.kt).

```kotlin
@Serializable
object MyFeature : Screen()
```

### 2. Create the ViewModel
Inject [RunActiveAdbShellCommandUseCase.kt](https://github.com/joaomgcd/ADBCommandCenter/blob/main/app/src/main/java/com/joaomgcd/adbcommandcenter/adb/connection/domain/RunActiveAdbShellCommandUseCase.kt) to execute shell commands against the active connection.

```kotlin
@HiltViewModel
class MyFeatureViewModel @Inject constructor(
    private val runShellCommand: RunActiveAdbShellCommandUseCase
) : ViewModel() {
    fun execute() {
        viewModelScope.launch {
            val result = runShellCommand("pm list features")
            // Handle result
        }
    }
}
```

### 3. Register the Screen
Add the composable destination to the `NavHost` in [AdbControlCenterApp.kt](https://github.com/joaomgcd/ADBCommandCenter/blob/main/app/src/main/java/com/joaomgcd/adbcommandcenter/ui/AdbControlCenterApp.kt).

```kotlin
composable<Screen.MyFeature> {
    MyFeatureScreen(onBackPressed = onBackPressed)
}
```

### 4. Add to Dashboard
Add your feature to the `features` list in [MainScreen.kt](https://github.com/joaomgcd/ADBCommandCenter/blob/main/app/src/main/java/com/joaomgcd/adbcommandcenter/main/ui/MainScreen.kt) to make it appear on the home grid.

```kotlin
private val features = listOf(
    // ... existing features
    DashboardFeature("My Feature", Icons.Default.Star, Screen.MyFeature)
)
```

---

## Tasker Plugin Integration

The app provides a seamless Tasker integration. You can find an existing example in the **ADB Shell Command** action defined in [TaskerShellCommand.kt](https://github.com/joaomgcd/ADBCommandCenter/blob/main/app/src/main/java/com/joaomgcd/adbcommandcenter/adb/shell/tasker/TaskerShellCommand.kt).

### Example: ADB Shell Command
The `TaskerShellCommandRunner` demonstrates how to bridge Tasker to the ADB logic:
1.  **Input**: Accepts a `command` string from Tasker.
2.  **Execution**: Uses `RunActiveAdbShellCommandUseCase` to run the command.
3.  **Output**: Returns the shell output to Tasker as a `%result` variable.

### How to Add a New Tasker Action


#### 1. Define IO DTOs
Define the input fields and output variables for Tasker using the library annotations.

```kotlin
@TaskerInputRoot
class MyActionInput @JvmOverloads constructor(
    @field:TaskerInputField("key") var value: String = ""
)

@TaskerOutputObject
class MyActionOutput(
    @get:TaskerOutputVariable("result") var result: String?
)
```

#### 2. Create the Hilt EntryPoint
Since Tasker runners are instantiated by the system, use an `@EntryPoint` to access UseCases.


```kotlin
@EntryPoint
@InstallIn(SingletonComponent::class)
interface MyActionEntryPoint {
    val runShell: RunActiveAdbShellCommandUseCase
}
```
#### 3. Implement the Runner
Extend [TaskerPluginRunnerAdbCommandCenter.kt](https://github.com/joaomgcd/ADBCommandCenter/blob/main/app/src/main/java/com/joaomgcd/adbcommandcenter/adb/tasker/runner/TaskerPluginRunnerAdbCommandCenter.kt). This handles the synchronous execution bridge for Tasker.

```kotlin
class MyActionRunner : TaskerPluginRunnerAdbCommandCenter<MyActionInput, MyActionOutput, MyActionEntryPoint>() {
    override suspend fun runSuspend(
        context: Context, 
        entryPoint: MyActionEntryPoint, 
        input: TaskerInput<MyActionInput>
    ): Result<MyActionOutput> {
        return entryPoint.runShell("echo ${input.regular.value}")
            .map { MyActionOutput(it) }
    }
}
```
#### 4. Boilerplate Helper
Link the runner and IO classes for the Tasker library.

```kotlin
class MyActionHelper(config: TaskerPluginConfig<MyActionInput>) :
    TaskerPluginConfigHelper<MyActionInput, MyActionOutput, MyActionRunner>(config) {
    override val runnerClass = MyActionRunner::class.java
    override val inputClass = MyActionInput::class.java
    override val outputClass = MyActionOutput::class.java
}
```

#### 5. Configuration UI
Extend [TaskerPluginConfigActivity.kt](https://github.com/joaomgcd/ADBCommandCenter/blob/main/app/src/main/java/com/joaomgcd/adbcommandcenter/adb/tasker/ui/TaskerPluginConfigActivity.kt) and [TaskerPluginConfigViewModel.kt](https://github.com/joaomgcd/ADBCommandCenter/blob/main/app/src/main/java/com/joaomgcd/adbcommandcenter/adb/tasker/ui/TaskerPluginConfigViewModel.kt) to provide the Compose-based configuration screen.

**ViewModel:**
```kotlin
@HiltViewModel
class MyActionViewModel @Inject constructor() : TaskerPluginConfigViewModel<MyActionInput>() {
    override val defaultInput = MyActionInput()
    override val defaultTitle = "My Custom Action"

    fun onValueChanged(newValue: String) {
        updateInput(MyActionInput(newValue))
    }
}
```

**Activity:**
```kotlin
@AndroidEntryPoint
class MyActionActivity : TaskerPluginConfigActivity<MyActionInput, MyActionViewModel>() {
    override val taskerHelper by lazy { MyActionHelper(this) }
    override val viewModel: MyActionViewModel by viewModels()

    @Composable
    override fun ColumnScope.Content(viewModel: MyActionViewModel) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        
        OutlinedTextField(
            value = uiState.input.value,
            onValueChange = { viewModel.onValueChanged(it) },
            label = { Text("Input Value") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
```

*   **Hilt Note**: Use `@AndroidEntryPoint` and `@HiltViewModel` to ensure dependencies are injected correctly during the Tasker configuration flow.

#### 6. Manifest Registration
Register the activity in [AndroidManifest.xml](https://github.com/joaomgcd/ADBCommandCenter/blob/main/app/src/main/AndroidManifest.xml) with the required intent filter:

```xml
<intent-filter>
    <action android:name="com.twofortyfouram.locale.intent.action.EDIT_SETTING" />
</intent-filter>
```
