# FunkyMercado

Tienda de decoración del mercado de **FUNKY PUNKY MC** (Paper 26.3, Java 25).

- **Solo vende, nunca compra:** cada compra destruye dinero (sumidero de la economía).
- Precio techo a propósito: la tienda del servidor es el último recurso; lo normal es comprar en los puestos de los jugadores.
- Solo funciona **dentro del mercado** (caja `zona` del config = región `mercado` de WorldGuard).
- Revisa espacio y saldo **antes** de cobrar; confirmación antes de cada compra.
- **Velocidad II dentro del mercado** (1.1): se renueva cada segundo y al salir se acaba sola; no quita otros efectos.

## Comandos
| Comando | Permiso | Qué hace |
| --- | --- | --- |
| `/deco` (`/decoracion`) | `funkymercado.usar` (todos) | Abre la tienda |
| `/deco <categoria>` | `funkymercado.usar` | Abre una categoría (lo usan los NPC) |
| `/fmercado info` | `funkymercado.admin` | Versión, economía, zona y productos |
| `/fmercado recargar` | `funkymercado.admin` | Relee `config.yml` |

NPC de FancyNpcs: `/npc action <npc> RIGHT_CLICK add player_command deco colores`

## Archivos
- `plugins/FunkyMercado/config.yml`: categorías, familias, productos, precios, zona y mensajes.
- `plugins/FunkyMercado/registro.log`: `COMPRO`, `ERROR_PAGO`, `ERROR_ENTREGA`.

## Compilar
GitHub Actions lo compila solo en cada push (Actions → Artifacts → `FunkyMercado`).
