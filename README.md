Project:InfinityMax-InfinityMaxIndustory(PIM-IMI)
=======

This template repository can be directly cloned to get you started with a new
mod. Simply create a new repository cloned from this one, by following the
instructions provided by [GitHub](https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-repository-from-a-template).

Once you have your clone, simply open the repository in the IDE of your choice. The usual recommendation for an IDE is either IntelliJ IDEA or Eclipse.

If at any point you are missing libraries in your IDE, or you've run into problems you can
run `gradlew --refresh-dependencies` to refresh the local cache. `gradlew clean` to reset everything 
{this does not affect your code} and then start the process again.

Mapping Names:
============
By default, the MDK is configured to use the official mapping names from Mojang for methods and fields 
in the Minecraft codebase. These names are covered by a specific license. All modders should be aware of this
license. For the latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

Developer Memo: 
==========
5) 追加・改変方法（増やす時はここだけ！）

アイテムを増やす
→ RegistryManager.registerItems() の addItem("your_id") を1行追加
    ↓
ブロック（非マシン）を増やす
→ RegistryManager.registerBlocks() の addSimpleBlock("your_id") を1行追加
    ↓
機械ブロックを増やす
    ↓
MachineBlock.Kind に 列挙子 を1つ追加
    ↓
RegistryManager.registerBlocks() に addMachine("your_block_id", MachineBlock.Kind.YOUR_KIND) を追加
→ 自動的に 同じ BlockEntityType に紐付きます（BEを個別化したい場合は別Typeを作るだけ）