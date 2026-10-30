# Estructura del proyecto

`services/` contiene aplicaciones independientes. Cada servicio tiene su propio `pom.xml`, clase de arranque, configuración y pruebas. El POM raíz sólo agrega módulos y centraliza versiones; ningún servicio depende de otro como dependencia Maven.

Los servicios que implementen dominio seguirán `domain/`, `application/` e `infrastructure/` dentro de su paquete base. No se crean clases vacías para representar capas aún no utilizadas.
