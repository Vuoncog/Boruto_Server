package com.example

import com.example.models.ApiResponse
import com.example.repository.HeroRepository
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.koin.core.context.stopKoin
import org.koin.java.KoinJavaComponent.inject
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    private val heroRepository: HeroRepository by inject(HeroRepository::class.java)

    @Test
    fun `access root, assert message`() {
        stopKoin()
        testApplication {
            val response = client.get("/")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("Welcome to Boruto Api!", response.bodyAsText())
        }
    }

    @Test
    fun `access all heroes, query non existing number, assert error`() {
        stopKoin()
        testApplication {
            val response = client.get("/boruto/heroes?page=6")
            assertEquals(HttpStatusCode.NotFound, response.status)
            assertEquals(
                expected = ApiResponse(
                    successful = false,
                    message = "Heroes not Found"
                ),
                actual = Json.decodeFromString(response.bodyAsText()))
        }
    }

    @Test
    fun `access all heroes, query invalid number, assert error`() {
        stopKoin()
        testApplication {
            val response = client.get("/boruto/heroes?page=a")
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(
                expected = ApiResponse(
                    successful = false,
                    message = "Number only allowed"
                ),
                actual = Json.decodeFromString(response.bodyAsText()))
        }
    }

    @Test
    fun `access all heroes, query page, assert successful information`() {
        withTestApplication(moduleFunction = Application::module) {
            val page = 1..5
            val heroesList = listOf(
                heroRepository.page1,
                heroRepository.page2,
                heroRepository.page3,
                heroRepository.page4,
                heroRepository.page5,
            )
            page.forEach {
                handleRequest(HttpMethod.Get, "/boruto/heroes?page=$it").apply {
                    assertEquals(HttpStatusCode.OK, response.status())

                    val expectedResponse = ApiResponse(
                        successful = true,
                        message = "found",
                        prevPage = calculatePage(it)["prevPage"],
                        nextPage = calculatePage(it)["nextPage"],
                        heroes = heroesList[it - 1]
                    )

                    val actualResponse = Json.decodeFromString<ApiResponse>(response.content.toString())
                    assertEquals(
                        expected = expectedResponse,
                        actual = actualResponse
                    )
                }


            }


        }
    }
}

private fun calculatePage(page: Int): Map<String, Int?> {
    var prevPage: Int? = null
    var nextPage: Int? = null
    if (page in 2..5) {
        prevPage = page.minus(1)
    }
    if (page in 1..4) {
        nextPage = page.plus(1)
    }
    return mapOf(
        "prevPage" to prevPage,
        "nextPage" to nextPage
    )
}
