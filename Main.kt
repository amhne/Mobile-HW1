package org.example

import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Scanner


data class User(
    val followers: Int,
    val following: Int,
    val created_at: String,
    val public_repos: List<String>
)

data class GitHubUserResponse(
    val followers: Int,
    val following: Int,
    val created_at: String,
    val public_repos: Int
)

data class Repo(
    val name: String
)

interface GitHubApi {
    @GET("users/{username}")
    fun getUserInfo(@Path("username") username: String): Call<GitHubUserResponse>

    @GET("users/{username}/repos")
    fun getUserRepos(@Path("username") username: String): Call<List<Repo>>
}

object RetrofitInstance {
    private const val BASE_URL = "https://api.github.com/"

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: GitHubApi = retrofit.create(GitHubApi::class.java)
}

val userCache = mutableMapOf<String, User>()

fun fetchUserData(username: String) {
    if (userCache.containsKey(username)) {
        println("User data loaded from memory: ${userCache[username]}")
        return
    }

    val call = RetrofitInstance.api.getUserInfo(username)
    call.enqueue(object : Callback<GitHubUserResponse> {
        override fun onResponse(
            call: Call<GitHubUserResponse>,
            response: Response<GitHubUserResponse>
        ) {
            if (response.isSuccessful) {
                val userResponse = response.body()
                userResponse?.let {
                    val repoCall = RetrofitInstance.api.getUserRepos(username)
                    repoCall.enqueue(object : Callback<List<Repo>> {
                        override fun onResponse(
                            call: Call<List<Repo>>,
                            response: Response<List<Repo>>
                        ) {
                            if (response.isSuccessful) {
                                val repos = response.body()
                                repos?.let {
                                    val user = User(
                                        followers = userResponse.followers,
                                        following = userResponse.following,
                                        created_at = userResponse.created_at,
                                        public_repos = it.map { repo -> repo.name }
                                    )
                                    userCache[username] = user
                                }
                            } else {
                                println("Error fetching repositories: ${response.errorBody()?.string()}")
                            }
                        }

                        override fun onFailure(call: Call<List<Repo>>, t: Throwable) {
                            println("Error with server connection: ${t.message}")
                        }
                    })
                }
            } else {
                println("Error fetching data: ${response.errorBody()?.string()}")
            }
        }

        override fun onFailure(call: Call<GitHubUserResponse>, t: Throwable) {
            println("Error with server connection: ${t.message}")
        }
    })
}

fun displayAllUsers() {
    if (userCache.isEmpty()) {
        println("No users in memory.")
    } else {
        userCache.forEach { println("Username: ${it.key}, Info: ${it.value}") }
    }
}

fun searchUserByUsername(username: String) {
    val user = userCache[username]
    if (user != null) {
        println("$user")
    } else {
        println("User not found.")
    }
}

fun searchRepoByName(repoName: String) {
    val found = userCache.filter { user ->
        user.value.public_repos.contains(repoName)
    }
    if (found.isEmpty()) {
        println("No repository found with this name.")
    } else {
        found.forEach { println("Username: ${it.key}, Info: ${it.value}") }
    }
}

fun main() {
    val scanner = Scanner(System.`in`)
    var isFirstTime = true
    while (true) {
        if(isFirstTime) {
            println("Please choose an option:")
            isFirstTime = false}
        else {
            println("\nPlease choose an option:")
        }
        println("1️⃣ Fetch user information")
        println("2️⃣ List of users in memory")
        println("3️⃣ Search by username from memory")
        println("4️⃣ Search by repository name from memory")
        println("5️⃣ Exit")

        when (scanner.nextInt()) {
            1 -> {
                println("Enter GitHub username:")
                val username = scanner.next()
                fetchUserData(username)
            }
            2 -> displayAllUsers()
            3 -> {
                println("Enter username to search:")
                val username = scanner.next()
                searchUserByUsername(username)
            }
            4 -> {
                println("Enter repository name to search:")
                val repoName = scanner.next()
                searchRepoByName(repoName)
            }
            5 -> {
                println("Exiting the program.")
                return
            }
            else -> println("Invalid option. Please try again.")
        }
    }
}