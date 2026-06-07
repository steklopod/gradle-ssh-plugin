## 🛡 [`online.colaba.ssh`](https://plugins.gradle.org/plugin/online.colaba.ssh) — Gradle plugin for easy deployment

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/online.colaba.ssh?label=Gradle%20Plugin%20Portal)](https://plugins.gradle.org/plugin/online.colaba.ssh)

Copy folders & files from your local machine (or CI runner) to a remote server over SSH, and run remote commands — with zero config, or fully customized.

Files land in the remote `~/${project.name}/...` folder.

### ⚙️ How it works

The plugin shells out to the **system OpenSSH** (`ssh` / `scp`) via `ProcessBuilder`. A single `ControlMaster`
connection is reused for every copy/command in a task run. There is **no bundled SSH library** (no JSch / groovy-ssh),
so any key the OS `ssh` understands works — **ed25519**, RSA, the OpenSSH key format, etc.

> **Requirements:** `ssh` and `scp` (OpenSSH) on `PATH` of the machine running the tasks
> (any CI runner and macOS/Linux dev box has them by default).

### 🎯 Quick start

In the root `build.gradle.kts`:

```kotlin
plugins {
    id("online.colaba.ssh") version "2.1.5"
}
group = "online.colaba"   // host is computed from group if not set explicitly
```

That's it. Run a copy:

```shell
gradle scp
```

Override the host per task:

```kotlin
tasks {
    scp { host = "my-domain.com" }
}
```

### 🔑 SSH key

The plugin looks for a private key named **`id_rsa`** — first in the **project root**, then in `~/.ssh/id_rsa`.
The file name is `id_rsa` regardless of key type; the contents may be **ed25519** (recommended) or RSA.

```shell
ssh-keygen -t ed25519 -C "deploy@my-server"          # modern key
ssh-copy-id -i ~/.ssh/id_ed25519.pub root@my-server  # authorize it on the server
cp ~/.ssh/id_ed25519 id_rsa                           # the plugin reads ./id_rsa
```

> 🔒 **Never commit `id_rsa`.** Gitignore it and, in CI, materialize it from a secret before the deploy step
> (e.g. `printf '%s\n' "${{ secrets.SSH_DEPLOY_KEY }}" > id_rsa && chmod 600 id_rsa`).

Host-key checking is disabled by default; set `checkKnownHosts = true` on a task to enable it.

### 🌀 Tasks

Generated automatically (one per subproject where relevant):

| Task | What it copies / does |
|------|-----------------------|
| `ssh` | base task, every option `false` by default |
| `scp` | base copy task, options enabled by default |
| `ssh-<subproject>` | the subproject's backend `build/libs/*.jar` (or whole folder) |
| `ssh-docker` | all compose / `Dockerfile` / `.dockerignore` files (incl. subprojects) |
| `ssh-gradle` | gradle wrapper + build files into every subproject |
| `ssh-vault` | `vault/` (config/agent/policies) + root `docker-compose.infra.yml` |
| `ssh-elastic` | whole `elastic/` folder |
| `ssh-broker` | whole `broker/` folder |
| `ssh-nginx` | whole `nginx/` folder |
| `ssh-monitoring` | whole `monitoring/` folder |
| `ssh-frontend-whole` | whole `frontend/` folder |
| `clear-frontend` | (local) remove `node_modules`, `.nuxt`, `.output`, lockfiles |

Docker helpers (run on the remote host):

| Task | What it does |
|------|--------------|
| `compose` | `docker compose up` all services (recreate + rebuild) |
| `compose-<svc>` | `docker compose up` a single service |
| `rollout-<svc>` | zero-downtime rollout: scale up new instance, await healthcheck, drain old |
| `ps` | list containers |
| `prune` | remove unused docker data |
| `cmd` | run an arbitrary local command (linux/windows) |

Run `gradle tasks` to see the full list under the `ssh` and `docker-main-${project.name}` groups.

### 🔮 Customization

Register your own task:

```kotlin
register("deployApi", online.colaba.Ssh::class) {
    host      = "my-domain.com"
    user      = "root"
    gradle    = true
    docker    = true
    nginx     = true
    directory = "distribution"
    run       = "cd ${project.name} && docker compose up -d --build"
}
```

Or tweak the defaults:

```kotlin
ssh {
    host = "my-domain.com"
    directory = "copy_me_to_remote"
    nginx = true
}
```

### 📋 Project structure example

```
project
 ├─ backend/        ← build/libs/*.jar, Dockerfile, compose.yml
 ├─ backend-2/      ← any number of backends
 ├─ frontend/       ← compose.yml, .output
 ├─ nginx/
 ├─ postgres/       ← backups/
 ├─ elastic/
 ├─ vault/
 ├─ gradlew, build.gradle.kts, docker-compose.yml
 └─ id_rsa          ← gitignored private key
```

The service name for every task equals `${project.name}` of the respective subproject.
