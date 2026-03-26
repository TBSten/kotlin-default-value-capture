# kotlin-default-value-capture

Kotlin の関数デフォルト引数の値を、コンパイル時に取得できるコンパイラプラグインです。

## モチベーション

Kotlin では関数にデフォルト引数を定義できますが、その値をプログラムから参照する標準的な手段がありません。
例えば CLI ツールや設定フレームワークで「デフォルト値をヘルプに表示したい」といったユースケースでは、値を二重管理するか、リフレクションに頼る必要があります。

このプラグインは `defaultArgOf` という関数呼び出しをコンパイル時に **デフォルト値の式そのもの** に差し替えることで、型安全かつゼロランタイムコストで解決します。

## セットアップ

### Gradle

```kotlin
// build.gradle.kts
plugins {
    id("me.tbsten.defaultargcapture") version "0.1.0-alpha01"
}
```

プラグインを適用するだけで、runtime ライブラリの依存追加やコンパイラプラグインの設定は自動で行われます。

## 使い方

```kotlin
import com.example.plugin.runtime.defaultArgOf

fun greet(name: String = "World") {
    println("Hello, $name!")
}

fun main() {
    // コンパイル時に "World" に置き換えられる
    val defaultName = defaultArgOf<String>(funName = "greet", argName = "name")
    println("デフォルト値: $defaultName") // => デフォルト値: World
}
```

### API

```kotlin
inline fun <reified T> defaultArgOf(funName: String, argName: String): T
```

| パラメータ | 説明 |
|---|---|
| `T` | デフォルト値の型 |
| `funName` | 対象関数の完全修飾名 |
| `argName` | 対象パラメータの名前 |

- 引数はすべて **コンパイル時定数** である必要があります
- 存在しない関数名・引数名や、デフォルト値のないパラメータを指定した場合は **コンパイルエラー** になります

### ユースケース例

```kotlin
// CLI ツールのヘルプ表示
fun serve(port: Int = 8080, host: String = "localhost") { /* ... */ }

fun printHelp() {
    val defaultPort = defaultArgOf<Int>(funName = "serve", argName = "port")
    val defaultHost = defaultArgOf<String>(funName = "serve", argName = "host")
    println("  --port  ポート番号 (デフォルト: $defaultPort)")
    println("  --host  ホスト名   (デフォルト: $defaultHost)")
}
```

## 既知の制限

| 制限 | 備考 |
|---|---|
| 同一モジュール内の関数のみ参照可能 | 別モジュールの関数のデフォルト値は取得できません |
| `funName` に完全修飾名が必要 | 短縮名では名前の衝突が起こる可能性があるため |

## 動作原理

1. ユーザーコードで `defaultArgOf<T>(funName = "f", argName = "x")` を呼び出す
2. FIR チェッカーが型解析フェーズで引数の妥当性を検証し、問題があればコンパイルエラーを報告
3. IR Transformer が呼び出しを見つけ、対象関数のデフォルト値 IR 式で差し替える

## 要件

- Kotlin 2.x (K2 コンパイラ)

## ライセンス

MIT
