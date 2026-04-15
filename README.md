# NottPay 💸

Plugin de pagos multi-moneda para servidores **Paper 1.20.4 – 1.21.8**, con soporte para **Vault** y **EdTools**, historial de transacciones en base de datos SQL y cooldowns configurables.

> **Autor:** nottabaker  
> **API:** Paper 1.20+  
> **Java:** 21  

---

## Características

- 💱 **Multi-moneda** — Soporta cualquier cantidad de monedas de Vault y EdTools simultáneamente
- 🛡️ **Anti-booster** — Los pagos con EdTools usan `setCurrency()` directo, evitando que los boosters multipliquen el monto recibido
- 🗄️ **Base de datos SQL** — SQLite (por defecto) o MySQL/MariaDB, con pool de conexiones HikariCP
- 📜 **Historial paginado** — Cada jugador puede ver todas sus transacciones enviadas y recibidas
- ⏱️ **Cooldowns configurables** — Por comando, con permiso de bypass para admins
- 🔤 **Abreviaciones de cantidad** — `10k`, `1.5m`, `2b`, etc.
- 🔄 **Reload en caliente** — Sin reiniciar el servidor
- 📣 **Pay All** — Envía moneda a todos los jugadores online desde consola o con permiso admin
- ✅ **Tab-completion inteligente** — Jugadores online, monedas disponibles y montos rápidos
- 🔧 **Comandos 100% configurables** — Nombre, aliases y permisos editables desde `config.yml`

---

## Comandos

| Comando | Descripción | Permiso |
|---------|-------------|---------|
| `/pay <jugador\|all> <moneda> <cantidad>` | Transfiere moneda a un jugador (o a todos) | `nottpay.pay` |
| `/transacciones [página]` | Ver historial de pagos | `nottpay.transactions` |
| `/nottpay reload` | Recarga la configuración en caliente | `nottpay.admin` |

> Los nombres y aliases de los comandos son configurables en `config.yml`.

---

## Permisos

| Permiso | Descripción | Default |
|---------|-------------|---------|
| `nottpay.pay` | Usar `/pay` | `true` |
| `nottpay.transactions` | Ver `/transacciones` | `true` |
| `nottpay.admin` | Usar `/nottpay reload` y `/pay all` | `op` |
| `nottpay.admin.payall` | Solo `/pay all` sin acceso a reload | `op` |
| `nottpay.bypass.cooldown` | Ignorar todos los cooldowns | `op` |

---

## Instalación

1. Descarga el `.jar` desde [Releases](../../releases) o compílalo tú mismo (ver abajo).
2. Colócalo en la carpeta `plugins/` de tu servidor Paper.
3. Reinicia el servidor. Se generarán `config.yml`, `messages.yml` y `currencies.yml`.
4. Configura tus monedas en `currencies.yml`.
5. *(Opcional)* Cambia a MySQL en `config.yml` si lo necesitas.

### Dependencias

| Plugin | Requerido |
|--------|-----------|
| [Vault](https://www.spigotmc.org/resources/vault.34315/) | Opcional — para monedas Vault |
| EdTools | Opcional — para monedas EdTools |

Al menos uno de los dos debe estar presente para que haya monedas disponibles.

---

## Configuración

### `config.yml`
```yaml
settings:
  prefix: "&6&lNottPay &8» &r"

pay-command:
  name: "pay"
  aliases: [pagar, transfer]
  permission: "nottpay.pay"

transaction-command:
  name: "transacciones"
  aliases: [transactions, historial]
  permission: "nottpay.transactions"
  per-page: 10

currency-formats:
  enabled: true
  formats:
    k: 1000
    m: 1000000
    b: 1000000000
    t: 1000000000000
    q: 1000000000000000
  allow-decimals: true

cooldowns:
  pay: 5          # segundos (0 = desactivado)
  transactions: 3

database:
  type: "SQLITE"  # SQLITE o MYSQL
  mysql:
    host: "localhost"
    port: 3306
    database: "nottpay"
    username: "root"
    password: ""
```

### `currencies.yml`
```yaml
currencies:
  money:
    display-name: "&aMonedas"
    provider: "vault"

  gems:
    display-name: "&bGemas"
    provider: "edtools"
    edtools-currency: "gems"
    bypass-booster: true   # true = los boosters NO aplican al recibir por /pay
```

> **`bypass-booster`** — Cuando es `true` (por defecto), el depósito usa `setCurrency()` directamente en lugar de `addCurrency()`, evitando que el `EdToolsCurrencyAddEvent` dispare los boosters. Ponlo en `false` si quieres que los boosters apliquen también en pagos.

### `messages.yml`
Todos los mensajes son editables. Soportan códigos de color `&` y los siguientes placeholders:

`{sender}`, `{receiver}`, `{amount}`, `{currency}`, `{balance}`, `{player}`, `{input}`, `{command}`, `{min}`, `{page}`, `{max_page}`, `{date}`, `{count}`, `{time}`

---

## Compilación

Requiere Java 21 y Git.

```bash
git clone https://github.com/MarcianoDeHolanda/NottPay.git
cd NottPay
./gradlew build
```

El JAR final se genera en `build/libs/NottPay-1.0.0.jar`.

> **Nota:** Las librerías de EdTools deben estar presentes en la carpeta `Lib/` para compilar. No se incluyen en el repositorio por ser privadas.

---

## Base de datos

### SQLite (por defecto)
No requiere configuración extra. El archivo `data/database.db` se crea automáticamente en la carpeta del plugin.

### MySQL / MariaDB
Cambia en `config.yml`:
```yaml
database:
  type: "MYSQL"
  mysql:
    host: "tu-host"
    port: 3306
    database: "nottpay"
    username: "usuario"
    password: "contraseña"
```
Compatible con MySQL 5.7+ y cualquier versión de MariaDB.

---

## Estructura del proyecto

```
src/main/java/ve/nottabaker/nottpay/
├── NottPay.java                        # Clase principal
├── command/
│   ├── PayCommand.java                 # /pay
│   ├── TransactionCommand.java         # /transacciones
│   └── NottPayCommand.java             # /nottpay
├── config/
│   └── ConfigManager.java              # Gestión de YAMLs
├── currency/
│   ├── CurrencyManager.java            # Registro de monedas
│   ├── CurrencyProvider.java           # Interfaz de proveedor
│   └── provider/
│       ├── VaultProvider.java
│       └── EdToolsProvider.java
├── transaction/
│   ├── Transaction.java                # Modelo de transacción
│   └── TransactionManager.java        # Persistencia SQL (HikariCP)
└── util/
    ├── AmountParser.java               # Parser de abreviaciones (10k, 1.5m...)
    └── CooldownManager.java            # Cooldowns en memoria
```

---

## Licencia

Este proyecto es de uso privado. No redistribuir sin permiso del autor.
