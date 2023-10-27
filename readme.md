## ProjectLocationUpdater

参考：
https://mvnrepository.com/artifact/org.eclipse.platform/org.eclipse.core.resources/3.19.100

## 使用

#### 调试

1. 在.project的目录下把需要变更的工程名称列出来：

```
.projects$ ls | xargs echo

```

2. 然后再运行导入工程并运行，把 原来的工作空间的绝对路径 和 上面列出来的字符串 一起作为参数传给 LocationUpdater ，同时修改 LocationUpdater当前工作目录 到新的工作空间的位置。

#### 运行

先随便填一个不存在的旧workspace，把旧的路径打印出来

```
winse@DESKTOP-EM57G48:pub-2022$ /mnt/e/workspaces/java/ProjectLocationUpdater/wsl-eclipse-workspace-migration.sh c:/123 .
c:/123
bigendian-executors~|~C:/bigendian/octopus/pub-2022/octopus/bigendian-executors ~~ c:/123 -> D:\frameworks\bigendian\octopus\pub-2022
...

```

再执行路径更新：

```
winse@DESKTOP-EM57G48:pub-2022$ /mnt/e/workspaces/java/ProjectLocationUpdater/wsl-eclipse-workspace-migration.sh C:/bigendian/octopus/pub-2022 .

```
