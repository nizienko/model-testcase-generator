package com.github.nizienko.example

import com.github.nizienko.core.*


fun main() {
    val tv = model("TV") {
        entryAction(title = "Turn on the TV", expectedResult = "TV is on", leadTo = "Channel mode")
    }

    tv["Channel mode"] has {
        this act "Press some channel" expect "Channel switched" at "Channel mode"
        this act "Press Menu button" expect "Menu appeared" at "Menu mode"
        this act "Press Power button" expect "TV switched off" at "Switched off"
        this act "Press Volume up button" expect "Volume became louder" at "Channel mode"
        this act "Press Volume down button" expect "Volume became more silent" at "Channel mode"
    }

    tv["Menu mode"] has {
        this act "Press Back button" expect "Went back to Channel" at "Channel mode"
        this act "Select another language" expect "Language changed" at "Menu mode"
    }

    tv["Switched off"] has {
        this act "Press Power button" expect "TV switched on" at "Channel mode"
    }

    tv.export()
}
