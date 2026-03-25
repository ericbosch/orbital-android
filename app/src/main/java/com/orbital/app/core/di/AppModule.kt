package com.orbital.app.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.orbital.app.core.network.OrbitalApiClient
import com.orbital.app.data.AppearanceStore
import com.orbital.app.data.OrbitalRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "orbital_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(15, TimeUnit.SECONDS)
                pingInterval(20, TimeUnit.SECONDS)
                // Keep websocket streams alive for long-running model responses.
                readTimeout(0, TimeUnit.MILLISECONDS)
                writeTimeout(0, TimeUnit.MILLISECONDS)
                callTimeout(0, TimeUnit.MILLISECONDS)
            }
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(WebSockets) {
            maxFrameSize = Long.MAX_VALUE
        }
        install(Logging) { level = LogLevel.BODY }
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> = ctx.dataStore

    @Provides
    @Singleton
    fun provideOrbitalApiClient(client: HttpClient): OrbitalApiClient = OrbitalApiClient(client)

    @Provides
    @Singleton
    fun provideAppearanceStore(dataStore: DataStore<Preferences>): AppearanceStore =
        AppearanceStore(dataStore)

    @Provides
    @Singleton
    fun provideOrbitalRepository(
        apiClient: OrbitalApiClient,
        dataStore: DataStore<Preferences>
    ): OrbitalRepository = OrbitalRepository(apiClient, dataStore)
}
