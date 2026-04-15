<div align="center">
  <img width="350px" src="https://cdn.modrinth.com/data/cached_images/929a612be7724be2060f641975a41c0b4950ea8f_0.webp">
</div>

Wynnscribe is a mod that aims to translate the Wynncraft server into various languages.
Currently, it supports Japanese.

WynnscribeはWynncraftのサーバーを様々な言語に翻訳することを目的としたmodです。

現在は日本語のみ対応しています。

日本語で使用するには言語設定を"日本語/Japanese"に設定する必要があります。

また、翻訳にご協力頂ける場合は是非[こちら(https://studio.wynnscribe.com)](https://studio.wynnscribe.com)からお願いします！

# 必要Mod (0.1.1時点)
[Fabric API](https://modrinth.com/mod/fabric-api)<br/>
[Wynntils](https://modrinth.com/mod/wynntils/)

## 【上級者向け/非推奨】Geminiによる自然な翻訳機能
<details>
<summary>特段の理由があり、どうしても利用したい場合</summary>

**本機能はMod開発者向けの補助機能であるため、デフォルトでは無効になっています。**
**このModの開発者以外の利用を一切想定しません。**

通常、未対応のテキストはGoogle翻訳で処理されます。環境変数 `WYNNSCRIBE_GEMINI_API_KEY` を設定することでGeminiを用いた翻訳に切り替えることも可能ですが、有効化には専門的な手順が必要となる上、このModの開発者及び翻訳者以外の利用を一切想定していないため一般のご利用は推奨しません。

**動作の仕組みとAPIキーの取り扱い**
もし独自にご利用になる場合は、以下の仕組みをご確認ください。

- **通信について**: 翻訳処理を行う都合上、入力されたAPIキーは一時的にWynnscribeのサーバーを経由して処理されます。
- **保存について**: キーは翻訳の通信にのみ使用されます。Wynnscribeのサーバー上に保存されたり、記録に残ることは一切ありません。
- **APIキーの設定**: ご利用の際は、念のためGoogle AI Studio側でAPIキーにリクエスト上限や利用額の制限を設けておくと、より安心してご利用いただけます。
- **翻訳結果の利用**: この機能を用いて行われた翻訳は一般ユーザーも利用できるように公開されます。

**最後に**
本機能はあくまで開発時の翻訳作業を想定したものです。通常はデフォルト設定（Google翻訳）のままで十分にお楽しみいただけますので、特段の理由がない限りはそのままご利用ください。
</details>
