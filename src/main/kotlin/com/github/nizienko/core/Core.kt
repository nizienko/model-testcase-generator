package com.github.nizienko.core

import java.nio.file.Paths
import kotlin.text.StringBuilder

fun model(projectName: String, function: Model.() -> Unit): Model {
    return Model(projectName).apply(function)
}

class Test(val title: String, val steps: List<Action>) {
    override fun toString(): String {
        return StringBuilder().apply {
            append("Test: $title\n")
            var n = 1
            steps.forEach {
                append("$n. ${it.title} --> ${it.leadTo}\n")
                n++
            }
            append("--------------\n")
        }.toString()
    }
}

class Model(private val projectName: String) {
    private lateinit var entryAction: Action
    private val states = mutableMapOf<String, State>()

    fun entryAction(title: String, leadTo: String) {
        entryAction = Action(title, leadTo)
    }

    fun state(title: String, function: State.() -> Unit) {
        val state = State(title).apply(function)
        states[title] = state
    }

    override fun toString(): String {
        return "entry: $entryAction. states:[$states]"
    }

    fun drawUml(): String = StringBuilder().apply {
        append("@startuml\n")
        append("User --> (${entryAction.leadTo}) : ${entryAction.title}\n")
        states.values.forEach { state ->
            state.actions.forEach { action ->
                append("(${state.title}) --> (${action.leadTo}) : ${action.title}\n")
            }
        }
        append("@enduml\n")
    }.toString()

    fun generate(): List<Test> {
        val entryState =
            states[entryAction.leadTo] ?: throw IllegalMonitorStateException("Define state ${entryAction.leadTo}")

        val actionsCreated = mutableListOf<Action>()
        val actionsChecked = mutableSetOf<Action>()
        val entryActionProcess = ActionProcess(actionsCreated, actionsChecked, entryAction)
        val tests = mutableListOf<List<Action>>()
        while (entryActionProcess.hasSomethingToCheck()) {
            val steps = mutableListOf<Action>()
            var currentAction = entryActionProcess
            steps.add(currentAction.action)
            actionsChecked.add(currentAction.action)
            while (currentAction.hasSomethingToCheck()) {
                currentAction = currentAction.nextAction()
                steps.add(currentAction.action)
                actionsChecked.add(currentAction.action)
            }
            tests.add(steps)
        }
        // create titles
        val allSteps = tests.flatten().map { it.title }.toList()
        val suite = mutableListOf<Test>()
        tests.forEach { steps ->
            val title = steps.sortedBy { step -> allSteps.filter { it == step.title }.size }.first().title
            suite.add(Test(title, steps))
        }
        return suite
    }

    fun export() {
        val dir = Paths.get("out").resolve("tests").resolve(projectName)
        dir.toFile().mkdirs()
        dir.resolve("testCases.txt").toFile().writeText(StringBuilder().apply { generate().forEach { append(it.toString() + "\n") } }.toString())
        dir.resolve("model.puml").toFile().writeText(drawUml())
    }


    inner class ActionProcess(
        private val actionsCreated: MutableList<Action>,
        private val actionsChecked: MutableSet<Action>,
        val action: Action
    ) {
        init {
            actionsCreated.add(action)
        }

        private val state = states[action.leadTo] ?: throw IllegalMonitorStateException("Define state ${action.leadTo}")
        private val followedActions: List<ActionProcess> by lazy {
            state.actions.filter { actionsCreated.contains(it).not() }.map {
                ActionProcess(
                    actionsCreated,
                    actionsChecked,
                    it
                )
            }
        }

        fun hasSomethingToCheck(): Boolean {
            return followedActions.any {
                actionsChecked.contains(it.action).not()
            } || followedActions.any { it.hasSomethingToCheck() }
        }

        fun nextAction(): ActionProcess {
            return followedActions.first { it.hasSomethingToCheck() || actionsChecked.contains(it.action).not() }
        }
    }
}


class State(val title: String) {
    val actions = mutableListOf<Action>()

    fun action(title: String, leadTo: String) {
        actions.add(Action(title, leadTo))
    }

    override fun toString(): String {
        return "State(title='$title', actions=$actions)"
    }

}

class Action(val title: String, val leadTo: String) {
    override fun toString(): String {
        return "Action(title='$title', leadTo=$leadTo)"
    }
}