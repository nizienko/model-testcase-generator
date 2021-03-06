package com.github.nizienko.example

import com.github.nizienko.core.*

fun main() {
    model("testsite.com") {
        entryAction("Открыть в браузере сайт", null, "Главная страница. Не авторизирован")
        entryAction("Открыть в браузере сайт", null, "Главная страница. Не авторизирован")

        state("Главная страница. Не авторизирован") {
            action("Ввести в форму логина валидные креды", "Главная страница. Пользователь Авторизован")
            action(
                "Ввести в форму логина невалидные креды",
                "Отображается сообщение, что креды не валидны",
                "Главная страница. Не авторизирован"
            )
        }
        state("Главная страница. Пользователь Авторизован") {
            action("Совершить платеж на сумму в пределах лимита", "Страница успеха")
            action("Совершить платеж больше лимита", "Страница с ошибкой")
            action("Нажать пополнение кошелька", "Форма пополнения")
            action("Разлогиниться", "Главная страница. Не авторизирован")
            action("Открыть историю", "Страница истории")
         }
        state("Страница истории") {
            action("Выбрать платеж", "Информация о платеже")
            action("Выбрать пополнение", "Информация о пополнении")
            action("Вернуться на главную", "Главная страница. Пользователь Авторизован")
        }
        state("Информация о платеже") {
            action("Закрыть", "Страница истории")
        }
        state("Информация о пополнении") {
            action("Закрыть", "Страница истории")
        }
        state("Форма пополнения") {
            action("Нажать отмена", "Главная страница. Пользователь Авторизован")
            action("Пополнить в пределах лимита", "Страница успеха")
            action("Пополгить с превышением лимита", "Страница с ошибкой")
        }
        state("Страница успеха") {
            action("Вернуться на главную", "Главная страница. Пользователь Авторизован")
        }
        state("Страница с ошибкой") {
            action("Вернуться на главную", "Главная страница. Пользователь Авторизован")
        }
    }.export()
}