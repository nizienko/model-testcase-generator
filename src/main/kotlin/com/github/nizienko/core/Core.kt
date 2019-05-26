package com.github.nizienko.core

import guru.nidi.graphviz.*
import guru.nidi.graphviz.attribute.*
import guru.nidi.graphviz.engine.Format
import java.io.File
import java.nio.file.Paths
import javax.swing.SpringLayout.NORTH
import javax.swing.SpringLayout.SOUTH
import kotlin.text.StringBuilder


fun model(projectName: String, function: Model.() -> Unit = {}): Model {
    return Model(projectName).apply(function)
}

class Test(val title: String, val steps: List<Action>) {
    override fun toString(): String {
        return StringBuilder().apply {
            append("Test: $title\n")
            var n = 1
            steps.forEach { a ->
                append("$n. ${a.title} --> ${a.expextedResult?.let { it + ". " + a.leadTo } ?: a.leadTo}\n")
                n++
            }
            append("--------------\n")
        }.toString()
    }
}

infix operator fun Model.get(title: String) = NotFinishedState(this, title)
class NotFinishedState(val model: Model, val title: String)

infix fun NotFinishedState.has(function: State.() -> Unit) {
    val state = State(title).apply(function)
    model.states[title] = state
}

infix fun State.act(title: String) = NotFinishedAcction(this, title)
class NotFinishedAcction(val state: State, val title: String)

infix fun NotFinishedAcction.expect(expectingResult: String) =
    NotFinishedAcctionWithExpectingResult(state, title, expectingResult)

class NotFinishedAcctionWithExpectingResult(val state: State, val title: String, val expectedResult: String)

infix fun NotFinishedAcctionWithExpectingResult.at(state: String) {
    this.state.actions.add(Action(title, expectedResult, state))
}

class Model(private val projectName: String) {
    private lateinit var entryAction: Action
    val states = mutableMapOf<String, State>()

    fun entryAction(title: String, expectedResult: String? = null, leadTo: String) {
        entryAction = Action(title, expectedResult, leadTo)
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
        append("package $projectName {\n")
        append("User -> (${entryAction.leadTo}) : ${entryAction.title}\n")
        states.values.forEach { state ->
            state.actions.forEach { action ->
                append("(${state.title}) --> (${action.leadTo}) : ${action.title}\n")
            }
        }
        append("}\n")
        append("@enduml\n")
    }.toString()

    fun drawImage(file: File) {
        graph(directed = true) {
            edge["color" eq "blue", Arrow.VEE, Style.DOTTED]
            node[Color.BLUE, Shape.RECTANGLE]
            "User"[Color.YELLOW, Style.FILLED, Shape.CIRCLE]
            graph[RankDir.TOP_TO_BOTTOM]
//            entryAction.title[Shape.RECTANGLE, Color.GREEN]
            ("User" -  entryAction.leadTo)[Label.html("<font color='blue'>${entryAction.title}</font>")]
            states.values.forEach { state ->
                state.actions.forEach { action ->
//                    action.title[Shape.RECTANGLE, Color.GREEN]
                    (state.title - action.leadTo)[Label.html("<font color='blue'>${action.title}</font>")]
                }
            }
        }.toGraphviz().render(Format.PNG).toFile(file)
    }


    fun generate(): List<Test> {
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
        val dir = Paths.get("out").resolve("testcases").resolve(projectName)
        dir.toFile().mkdirs()
        val tests = generate()
        val uml = drawUml()
        dir.resolve("testcases.txt").toFile()
            .writeText(StringBuilder().apply { tests.forEach { append(it.toString() + "\n") } }.toString())
        dir.resolve("model.puml").toFile().writeText(uml)

        dir.resolve("testcases.md").toFile().writeText(StringBuilder().apply {
            append("![$projectName](model.png?raw=true)\n\n")
            var s = 1
            tests.forEach { test ->
                append("$s ${test.title}\n\n")
                append("| | Step | Expected result |\n")
                append("|-----:|:-----|-----:|\n")
                var n = 1
                test.steps.forEach { a ->
                    append("| $n | ${a.title} | ${a.expextedResult ?: a.leadTo} |\n")
                    n++
                }
                s++
                append("\n")
            }
        }.toString())
        drawImage(dir.resolve("model.png").toFile())
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


    fun action(title: String, leadTo: String = this@State.title) {
        actions.add(Action(title, null, leadTo))
    }

    fun action(title: String, expectedResult: String, leadTo: String = this@State.title) {
        actions.add(Action(title, expectedResult, leadTo))
    }

    override fun toString(): String {
        return "State(title='$title', actions=$actions)"
    }

}

class Action(val title: String, val expextedResult: String?, val leadTo: String) {
    override fun toString(): String {
        return "Action(title='$title', expectedResult='$expextedResult', leadTo=$leadTo)"
    }
}