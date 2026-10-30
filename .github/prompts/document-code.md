# PROMPT 03.1 — Documentación profesional del código Java mediante JavaDoc y comentarios inline

## 1. Rol

Actúa como un Senior Java Developer especializado en documentación técnica, Clean Code, SOLID y arquitectura de microservicios con Spring Boot.

Tu responsabilidad es revisar y documentar el código Java existente en el backend, utilizando estándares profesionales de JavaDoc y comentarios inline cuando sean necesarios.

El objetivo es mejorar la legibilidad, mantenibilidad y comprensión del código para cualquier desarrollador que se incorpore al proyecto.

## 2. Contexto

El backend utiliza:

- Java 21.
- Spring Boot.
- Spring Cloud Gateway.
- Spring WebFlux.
- Maven.
- Arquitectura de microservicios.
- Arquitectura hexagonal como estándar para los microservicios de negocio.

Actualmente existen los módulos:

- `api-gateway`.
- `auth-service`.

El API Gateway ya cuenta con su implementación inicial de routing, filtros, Correlation ID, auditoría HTTP, manejo de errores y configuraciones transversales, según el estado actual del repositorio.

Es necesario documentar el código que ya existe antes de continuar desarrollando nuevas funcionalidades.

## 3. Objetivo principal

Revisar y documentar todo el código fuente Java existente dentro del directorio `backend`.

La documentación deberá estar escrita en español y utilizar los estándares de JavaDoc.

También deberán agregarse comentarios inline cuando aporten información técnica relevante.

No se busca llenar el código de comentarios innecesarios, sino documentar correctamente las responsabilidades, contratos, decisiones y comportamientos importantes.

## 4. Análisis previo obligatorio

Antes de modificar archivos:

1. Inspecciona todos los módulos del backend.
2. Identifica las clases Java existentes.
3. Identifica interfaces, implementaciones, servicios, controladores, filtros y configuraciones.
4. Revisa las responsabilidades de cada componente.
5. Identifica métodos públicos y contratos relevantes.
6. Identifica comportamientos complejos que requieran explicación.
7. Revisa si ya existe documentación para evitar duplicaciones.
8. Verifica que la documentación describa el comportamiento real del código.

No agregues comentarios basándote únicamente en el nombre de una clase o método. Comprende primero su implementación.

## 5. Estándar JavaDoc obligatorio

Documenta las clases e interfaces relevantes utilizando JavaDoc.

Cada clase o interfaz deberá explicar:

- Qué responsabilidad tiene.
- Qué problema resuelve.
- A qué componente o capa pertenece.
- Cómo participa dentro de la arquitectura.
- Qué dependencias o colaboraciones relevantes mantiene, cuando sea necesario.

Ejemplo:

```java
/**
 * Filtro global encargado de gestionar la trazabilidad de las solicitudes HTTP
 * que atraviesan el API Gateway.
 *
 * <p>Garantiza que cada solicitud disponga de un identificador de correlación
 * que permita relacionar los eventos generados durante su procesamiento.</p>
 *
 * <p>El identificador se propaga hacia el servicio de destino y se incluye
 * en la respuesta enviada al cliente.</p>
 */
```

### Métodos

Documenta los métodos públicos y aquellos métodos internos cuya responsabilidad o comportamiento no resulte evidente.

Cuando corresponda, incluye:

- `@param`: descripción de los parámetros.
- `@return`: descripción del resultado.
- `@throws`: excepciones que puede lanzar.
- `@see`: referencias relacionadas.
- `@since`: únicamente cuando exista una política de versiones que justifique su uso.

Ejemplo:

```java
/**
 * Genera o recupera el identificador de correlación de una solicitud HTTP.
 *
 * @param request solicitud HTTP recibida por el Gateway
 * @return identificador único utilizado para la trazabilidad
 */
```

No agregues etiquetas innecesarias cuando no aporten información.

## 6. Comentarios inline

Agrega comentarios inline exclusivamente cuando ayuden a comprender:

- Decisiones arquitectónicas.
- Reglas de negocio o técnicas.
- Algoritmos no triviales.
- Condiciones complejas.
- Comportamientos reactivos que puedan resultar difíciles de interpretar.
- Manejo de errores no convencional.
- Restricciones de seguridad.
- Optimizaciones de rendimiento.
- Soluciones implementadas para evitar problemas conocidos.

Ejemplo:

```java
// Se conserva el Correlation ID recibido para mantener la trazabilidad
// de una solicitud que atraviesa diferentes componentes del sistema.
```

Evita comentarios redundantes como:

```java
// Incrementa el contador
counter++;
```

No utilices comentarios para compensar nombres deficientes o código difícil de entender. Si un método o variable tiene un nombre poco descriptivo, evalúa mejorar su nomenclatura sin alterar su comportamiento.

## 7. Documentación específica del API Gateway

Presta especial atención a los siguientes componentes:

### Routing

Documenta cómo se configuran las rutas y cómo se determina el servicio de destino.

### Correlation ID

Explica cómo se genera, valida, propaga y devuelve el identificador.

### Auditoría

Documenta qué información se registra, cuándo se genera el evento y qué información sensible se excluye.

### Filtros globales

Explica el propósito, orden de ejecución y responsabilidades de cada filtro.

### Manejo de errores

Documenta los tipos de errores contemplados y cómo se construyen las respuestas HTTP.

### CORS

Explica cómo se gestionan los orígenes y métodos autorizados.

### Configuración

Documenta las configuraciones importantes y las variables de entorno utilizadas.

## 8. Documentación específica de futuros microservicios

Aplica el mismo estándar a las clases existentes de `auth-service` y a cualquier otro microservicio que se incorpore posteriormente.

Cuando exista arquitectura hexagonal, documenta claramente:

- Dominio y reglas de negocio.
- Casos de uso.
- Puertos de entrada.
- Puertos de salida.
- Adaptadores.
- Implementaciones de persistencia.
- Integraciones externas.

La documentación deberá permitir comprender la dirección de las dependencias y la responsabilidad de cada componente.

## 9. Idioma y convenciones

Toda la documentación JavaDoc y los comentarios inline deberán escribirse en español.

Mantén en inglés:

- Nombres de clases.
- Nombres de métodos.
- Nombres de variables.
- Nombres de paquetes.
- Identificadores técnicos.
- Términos propios de frameworks y tecnologías.

Utiliza una redacción profesional, precisa y consistente.

Evita expresiones ambiguas, explicaciones excesivamente extensas y comentarios redundantes.

## 10. Restricciones

Esta tarea es exclusivamente de documentación.

- No modificar reglas de negocio.
- No modificar contratos de API.
- No cambiar firmas de métodos.
- No alterar el comportamiento de filtros.
- No modificar configuraciones.
- No cambiar dependencias Maven.
- No introducir nuevas funcionalidades.
- No realizar refactorizaciones funcionales.
- No eliminar comentarios que contengan información técnica relevante.
- No inventar comportamientos que el código no implemente.

Si detectas código que debería corregirse, pero cuya modificación excede el alcance de esta tarea, documenta el hallazgo en el informe final.

## 11. Validaciones obligatorias

Después de documentar:

1. Ejecuta la compilación Maven.
2. Verifica que JavaDoc no tenga errores.
3. Comprueba que las etiquetas JavaDoc estén correctamente utilizadas.
4. Verifica que no existan referencias inexistentes.
5. Ejecuta las pruebas existentes.
6. Confirma que no se hayan alterado contratos ni comportamientos.
7. Comprueba que la documentación coincida con la implementación real.

Cuando sea viable, ejecuta también la generación de JavaDoc mediante Maven.

No ignores errores de documentación generados por la herramienta.

## 12. Criterios de aceptación

La tarea se considerará terminada cuando:

- Las clases e interfaces relevantes estén documentadas.
- Los métodos públicos tengan JavaDoc apropiado.
- Los métodos complejos estén explicados cuando sea necesario.
- Existan comentarios inline donde aporten valor.
- La documentación esté escrita en español.
- No existan comentarios redundantes.
- No se haya alterado la lógica del sistema.
- La compilación y las pruebas sigan funcionando.

IMPORTANTE: documenta todo el código Java existente dentro del alcance del backend. No avances a nuevas funcionalidades ni continúes con otra fase.