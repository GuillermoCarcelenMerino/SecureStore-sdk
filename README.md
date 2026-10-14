# SecureStore

SDK de Android para guardar valores pequeños y sensibles (tokens, identificadores, preferencias privadas) **cifrados en el dispositivo**.

- Cifrado **AES-256/GCM** con una clave que vive en el **Android Keystore** y nunca sale de él.
- Persistencia con **Jetpack DataStore**, en la carpeta privada de la app.
- API con **corrutinas**: escrituras `suspend` y lecturas observables con `Flow`.
- Errores explícitos con `Result` y una jerarquía propia de excepciones.
- Sin dependencias de UI. `minSdk` 24.

> Proyecto de aprendizaje. Las decisiones de diseño están explicadas en [`securestore/DECISIONS.md`](securestore/DECISIONS.md).

## Instalación

El SDK todavía no está publicado en un repositorio Maven. Hay dos formas de usarlo:

**1. Composite build** (desarrollo, sin publicar). En el `settings.gradle.kts` de tu app:

```kotlin
includeBuild("../SecureStoreSDK")
```

y en el `build.gradle.kts` del módulo de la app:

```kotlin
dependencies {
    implementation("io.github.guillermocarcelenmerino:securestore")
}
```

**2. Maven local.** Requiere configurar `maven-publish` en el módulo `securestore` (pendiente) y ejecutar `./gradlew :securestore:publishToMavenLocal`.

Requisitos de la app: `minSdk` 24 o superior y `compileSdk` 36 o superior.

## Uso

### Crear el almacén (una sola vez)

Crea **una instancia por nombre** de almacén y reutilízala durante toda la vida de la app. Dos instancias con el mismo nombre a la vez no están soportadas (DataStore lo impide).

```kotlin
class MyApplication : Application() {
    val secureStore: SecureStore by lazy { SecureStore.init(this) }
}
```

Con Hilt:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object SecureStoreModule {
    @Provides
    @Singleton
    fun provideSecureStore(@ApplicationContext context: Context): SecureStore =
        SecureStore.init(context, SecureStoreConfig(name = "tokens"))
}
```

No lo crees dentro de un `@Composable`: se crearía una instancia nueva en cada recomposición.

### Guardar, leer y borrar

```kotlin
viewModelScope.launch {
    store.putString("auth_token", token)
        .onFailure { error -> /* ver "Errores" */ }

    val saved: String? = store.getString("auth_token").getOrNull()   // null si no existe

    store.remove("auth_token")
    store.clear()                                                    // borra todo el almacén
}
```

### Observar cambios

```kotlin
val token: StateFlow<String?> = store.observeString("auth_token")
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
```

El `Flow` emite el valor actual y cada cambio de esa clave (no emite cuando cambian otras claves).

### Varios almacenes

```kotlin
val tokens = SecureStore.init(context, SecureStoreConfig(name = "tokens"))
val settings = SecureStore.init(context, SecureStoreConfig(name = "settings"))
```

Cada nombre tiene su propio archivo y su propia clave de cifrado. **No cambies el nombre de un almacén existente**: los datos guardados con el nombre anterior dejarán de encontrarse.

## Errores

Todas las operaciones `suspend` devuelven `Result`. Los errores son siempre una `SecureStoreException`:

| Tipo | Cuándo ocurre |
|---|---|
| `EncryptionFailed` | Falla el cifrado o el descifrado (por ejemplo, datos manipulados o de otra clave). |
| `KeyInvalidated` | El sistema ha invalidado la clave del Keystore. El SDK borra la clave y los datos de ese almacén (ya irrecuperables) y la siguiente operación funciona con una clave nueva. |
| `StorageFailure` | No se ha podido leer o escribir el archivo. |

```kotlin
store.getString("auth_token").onFailure { error ->
    when (error) {
        is SecureStoreException.KeyInvalidated -> askUserToLogInAgain()   // los datos son irrecuperables
        is SecureStoreException.StorageFailure -> showRetry()
        else -> log(error)                                        // obligatorio: pueden añadirse tipos nuevos
    }
}
```

Incluye siempre una rama `else`: versiones futuras pueden añadir nuevos tipos de error. En `observeString`, los errores llegan por el `Flow` y se capturan con `.catch { }`.

## Cómo funciona

```
putString("token", "abc")
  → "abc" en bytes UTF-8
  → AES-256/GCM con la clave del Keystore "securestore_<name>"  →  IV (12 B) + datos + tag (16 B)
  → Base64
  → DataStore: datastore/<name>.preferences_pb
```

`getString` recorre el camino inverso. El archivo en disco solo contiene texto cifrado; sin la clave, que no se puede extraer del Keystore, no sirve de nada.

## Copias de seguridad

El backup de Android copia el archivo cifrado pero **no la clave del Keystore**. Si el usuario restaura la app en otro dispositivo, los datos llegarán pero no se podrán descifrar. Se recomienda excluir el archivo del backup en la app:

`res/xml/data_extraction_rules.xml` (Android 12+):

```xml
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="file" path="datastore/secureStore.preferences_pb" />
    </cloud-backup>
    <device-transfer>
        <exclude domain="file" path="datastore/secureStore.preferences_pb" />
    </device-transfer>
</data-extraction-rules>
```

y la misma exclusión en `res/xml/backup_rules.xml` (Android 11 o anterior). Usa el nombre de cada almacén que crees.

## R8 / ProGuard

No hace falta añadir reglas en la app: el SDK no usa reflexión y sus dependencias incluyen sus propias reglas. El archivo [`securestore/consumer-rules.pro`](securestore/consumer-rules.pro) se aplica automáticamente a las apps que usan el SDK. La app de ejemplo se compila en release con R8 activado para comprobarlo.

## Desarrollo

Estructura:

```
securestore/          el SDK (librería Android)
  cypher/             cifrado: Cypher + AndroidKeyStoreCypher
  storage/            almacenamiento: Storage + DataStoreStorage
  DefaultSecureStore  une cifrado y almacenamiento (internal)
  SecureStore.kt      API pública: interfaz, configuración y excepciones
app/                  app de ejemplo
```

Comandos (en Windows, `gradlew.bat`):

| Tarea | Comando |
|---|---|
| Compilar el SDK | `./gradlew :securestore:assembleDebug` |
| Tests de unidad | `./gradlew :securestore:testDebugUnitTest` |
| Tests instrumentados (emulador o dispositivo) | `./gradlew :securestore:connectedDebugAndroidTest` |
| Cobertura (informe HTML) | `./gradlew :securestore:koverHtmlReportDebug` |
| Cobertura mínima (falla si baja del 90 %) | `./gradlew :securestore:koverVerifyDebug` |
| App de ejemplo en release con R8 | `./gradlew :app:assembleRelease` |

Tests:

- **Unidad (JVM):** `DefaultSecureStore` con `FakeStorage` y `FakeCypher`, y `SecureStoreConfig`.
- **Instrumentados:** `AndroidKeyStoreCypher` (Keystore real), `DataStoreStorage` (archivos reales) y una prueba de integración que verifica que el archivo en disco no contiene el valor en claro.
- **Kover** mide los tests de unidad y excluye las clases que solo se ejecutan en un dispositivo, cubiertas por los tests instrumentados.

## Limitaciones

- Solo guarda `String`.
- Una instancia por nombre de almacén.
- Pensado para valores pequeños; no para archivos ni grandes volúmenes de datos.
- El Keystore protege la clave, pero no frente a código malicioso que se ejecute dentro de la propia app ni en dispositivos comprometidos.
