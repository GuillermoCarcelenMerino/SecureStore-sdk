# Decisiones de diseño — SecureStore

Registro de las decisiones tomadas en el SDK y su motivo. Cada entrada: qué se decidió y por qué.

## API pública

1. **Solo se guardan Strings.** Es un SDK de práctica: `getString`, `putString`, `observeString`, `remove` y `clear`. Otros tipos se pueden añadir después sin cambiar el cifrado ni el almacenamiento (ambos trabajan con bytes/texto).
2. **Interfaz + función de creación, no un `object` singleton.** `SecureStore` es una interfaz y se crea con `SecureStore.init(context, config)`. Permite varios almacenes, inyectarlo con Hilt y sustituirlo por un fake en los tests de quien lo use. Quien lo usa es responsable de crear **una sola instancia por nombre** (por ejemplo, en su `Application` o como `@Singleton`).
3. **`SecureStoreConfig.name` identifica el almacén.** De él salen el archivo de DataStore y el alias de la clave del Keystore, así que almacenes con nombres distintos no comparten datos ni clave. Su valor por defecto (`"secureStore"`) **no debe cambiar nunca**: los datos guardados con el nombre antiguo dejarían de encontrarse. Lo protege un test.
4. **Resultados con `Result`, no excepciones.** Las operaciones `suspend` devuelven `Result<T>`, así el error forma parte de la firma. Una clave inexistente es `Result.success(null)`, no un error. `observeString` devuelve un `Flow` (no es `suspend`) y sus errores llegan por el propio Flow.
5. **`explicitApi()`** obliga a declarar `public`/`internal` en todo; solo la interfaz, la configuración y las excepciones son públicas.

## Errores

6. **Excepciones públicas para capturarlas, pero solo el SDK las crea** (`internal constructor`). Tipos: `EncryptionFailed`, `KeyInvalidated`, `StorageFailure`. Cada una conserva la excepción original en `cause`.
7. **Siempre con rama `else` al hacer `when`:** se pueden añadir subtipos nuevos en versiones futuras y un `when` exhaustivo del usuario dejaría de compilar.
8. **Toda excepción interna se traduce** a una `SecureStoreException`; la cancelación de corrutinas (`CancellationException`) no se traduce, para que una corrutina cancelada siga cancelada.

## Cifrado

9. **Android Keystore + AES-256/GCM.** La clave se crea dentro del Keystore y nunca sale de él. GCM cifra y además detecta manipulaciones (falla al descifrar en lugar de devolver datos corruptos).
10. **IV aleatorio de 12 bytes por cada cifrado**, generado por Android y guardado delante del dato (`IV + cifrado + tag`). Nunca se reutiliza un IV con la misma clave.
11. **Alias de la clave: `securestore_<name>`**, una clave por almacén.
11b. **Recuperación de una clave invalidada.** Si Android invalida la clave (`KeyPermanentlyInvalidatedException`, por ejemplo al quitar el bloqueo de pantalla en claves que lo exigen), esa clave existe pero no se puede volver a usar, y sin recuperación el almacén quedaría bloqueado para siempre. Al detectarlo, el SDK borra la clave y los datos de ese almacén (ya ilegibles) y devuelve `KeyInvalidated` **esa vez**, para que la app sepa que se han perdido. La siguiente operación crea una clave nueva y el almacén vuelve a funcionar, vacío.
12. **Base64 para guardar el resultado como texto.** El cifrado produce bytes arbitrarios que no son texto válido; Base64 los representa sin pérdida. No aporta seguridad: la seguridad la da el cifrado.

## Almacenamiento

13. **DataStore Preferences** en la carpeta privada de la app, un archivo por almacén (`datastore/<name>.preferences_pb`). Es asíncrono (`suspend` + `Flow`), igual que la API.
14. **DataStore solo admite una instancia por archivo:** dos `SecureStore.init` con el mismo nombre a la vez lanzan `IllegalStateException`. Se documenta en el KDoc de `init` en lugar de mantener un registro global de instancias.

## Arquitectura interna

15. **`DefaultSecureStore` recibe `Storage` y `Cypher` como interfaces** (inyección de dependencias manual). El `Context` solo se usa en `SecureStore.init` para crear las implementaciones reales (`DataStoreStorage`, `AndroidKeyStoreCypher`). Así `DefaultSecureStore` se prueba en la JVM con `FakeStorage` y `FakeCypher`, sin mocks.
16. **Dependencias:** `kotlinx-coroutines-core` es `api` (el `Flow` aparece en la API pública); DataStore es `implementation` (detalle interno, se puede cambiar sin romper a nadie).
17. **`compileSdk` 36 en el SDK:** una librería obliga a las apps que la usan a compilar con un `compileSdk` igual o mayor, así que el SDK pide el mínimo que necesita.

## Calidad

18. **Tests en dos niveles.** Unidad (JVM) para `DefaultSecureStore` y `SecureStoreConfig`; instrumentados (dispositivo) para `AndroidKeyStoreCypher`, `DataStoreStorage` y un test de integración que comprueba que el archivo en disco no contiene el texto en claro.
19. **Kover con un mínimo del 90 % de líneas** sobre los tests de unidad (`koverVerifyDebug`). Se excluyen las clases que solo pueden ejecutarse en un dispositivo (`AndroidKeyStoreCypher`, `DataStoreStorage`, `SecureStore.Companion`), porque Kover no mide los tests instrumentados que las cubren.
20. **R8:** el SDK no necesita reglas en `consumer-rules.pro` (no usa reflexión). Comprobado compilando la app de ejemplo en release con R8 activado.

## Pendiente / limitaciones conocidas

- **Copias de seguridad:** el backup de Android copia el archivo cifrado pero no la clave del Keystore. Tras restaurar en otro dispositivo los datos no se pueden descifrar (`KeyInvalidated` o `EncryptionFailed`). Hoy se documenta en el README para que la app excluya el archivo del backup; queda pendiente decidir si el SDK debe borrar automáticamente los datos ilegibles.
- **Dispositivos rooteados o comprometidos:** el Keystore reduce el riesgo, pero no protege frente a código malicioso ejecutándose dentro de la propia app.
