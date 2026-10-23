# vytalCore

Plugin para Paper que reúne un sistema de menús YAML inspirado en el flujo de DeluxeMenus y funciones habituales de lobby inspiradas en DeluxeHub. Es una implementación independiente; no copia código de esos proyectos ni pretende cubrir cada una de sus funciones avanzadas.

## Requisitos
- Paper o Purpur 1.20.1 o superior.
- Java 17 para compilar. Minecraft/Paper 1.21 requiere Java 21 en el servidor.
- Velocity opcional para la acción `server:`. La acción utiliza el canal de mensajería `BungeeCord`, que Velocity puede admitir con `bungee-plugin-message-channel = true`.

## Compilar en GitHub
1. Crea un repositorio nuevo y sube el contenido de este proyecto.
2. Abre la pestaña **Actions** y ejecuta el workflow **Build vytalCore**, o haz un `push`.
3. Descarga el artefacto `vytalCore` de la ejecución terminada.
4. Coloca `vytalCore.jar` en `plugins/` del servidor Paper/Purpur y reinicia.

El workflow compila con Gradle instalado por GitHub Actions; no hace falta subir `gradlew`.

## Comandos
- `/vc help`
- `/vc reload`
- `/vc menu <nombre>`
- `/vc editor <nombre>`
- `/vc setspawn`

Permisos:
- `vytalcore.admin` — administración.
- `vytalcore.menu` — abrir menús.
- `vytalcore.bypass` — evita algunas protecciones del lobby.

## Menús YAML
Los archivos van en `plugins/vytalCore/menus/`. Cada archivo contiene `title`, `size` y `items`. Cada item admite `slot`, `material`, `name`, `lore` y `action`.

Acciones disponibles:
- `open:<menu>` — abre otro menú.
- `message:<texto>` — envía un mensaje.
- `player:<comando>` — ejecuta un comando como jugador.
- `console:<comando>` — ejecuta un comando como consola; `%player_name%` se sustituye.
- `server:<nombre>` — solicita conexión a un servidor de Velocity/BungeeCord.
- `sound:<NOMBRE_SOUND>` — reproduce un sonido Bukkit.
- `close` — cierra el inventario.
- `toggle-players` — oculta o muestra a los jugadores.

## Editor visual
`/vc editor <nombre>` abre una vista del menú. Haz clic en un ítem para moverlo al siguiente slot; el cambio se guarda en el YAML. Es un editor inicial y deliberadamente sencillo, no un constructor completo de acciones desde GUI.

## Protección de lobby
Edita `config.yml` para cambiar `hub.protected-worlds`, spawn, doble salto, ítems y scoreboard. El scoreboard es básico y puede expandir placeholders de PlaceholderAPI si el plugin está instalado.

## Compatibilidad
Se compila contra Paper API 1.20.1 para mantener el uso de API estable y evitar depender de APIs nuevas. La compatibilidad real debe probarse en cada versión concreta de Paper/Purpur; no se garantiza automáticamente en todas las futuras versiones. Para Paper 1.21+, ejecuta el servidor con Java 21.

## Limitaciones de esta versión inicial
- No incluye economía interna, tienda de cosméticos, hologramas nativos ni una implementación de todas las condiciones/acciones de DeluxeMenus.
- El editor visual actual solo permite mover un ítem al slot siguiente; para cambiar material, lore, permisos y acciones, edita el YAML.
- Las acciones `server:` requieren un proxy correctamente configurado.
- 
