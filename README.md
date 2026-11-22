# HugMe! · 抱抱

玩家之间可以互相抱抱：手持「抱抱券」右键其他玩家发起请求，对方在聊天栏点 `[同意]` 后开始动作，
周围玩家都能看到。

需要 [Player Animator](https://modrinth.com/mod/playeranimator) 作为前置（仅客户端需要）。

> English: right-click another player with a Hug Ticket to send a hug request; once they accept,
> both players play a hug animation that nearby players can see. [Player Animator](https://modrinth.com/mod/playeranimator)
> is required on the client.

## 环境

| 项 | 值 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.267 |
| Player Animator | 2.0.4+1.21.1 |
| JDK | 21（Gradle toolchain 自动下载） |
| Gradle | 用仓库自带的 wrapper（9.2.1） |

## 构建与运行

```bash
./gradlew build        # Windows: gradlew.bat build
# 产物：build/libs/HugMe!-NeoForge-1.21.1-<版本>.jar
./gradlew runClient    # 开发客户端
./gradlew runServer    # 开发服务端（首次启动前把 run/eula.txt 改成 eula=true）
./gradlew runData      # 数据生成（本模组目前没有生成器，留作以后用）
```

## 许可与致谢

- 代码与资源以 **MIT** 发布，版权声明见 [LICENSE](LICENSE)
- 动画库 [Player Animator](https://github.com/KosmX/minecraftPlayerAnimator) by KosmX（MIT）
- 动作数据、贴图与设计：TuYw
