# OraxenHostingService
## 1. 简介
Oraxen资源包自定义托管服务，目前支持123云盘和腾讯云对象存储服务。
## 2. 插件配置
### pan.parent-file-id 配置项配置方法
登录[123云盘网页端](https://www.123pan.com/)后，进入资源包上传路径，查看网页链接的`homeFilePath`变量，如下图所示：
![parent_file_id1.png](imgs/parent_file_id1.png)
文件夹id和`homeFilePath`变量的数字一一对应。上图中，如果要把资源包保存到`/pack/bc/`，配置项`parent-file-id`应填`8187024`。

**使用123云盘存放资源包时，务必开启对应文件夹的直链空间功能！！！**

其他请查看配置文件内注释。
## 3. 指令
| 指令          | 功能     | 权限                 |
|-------------|--------|--------------------|
| /ohs reload | 重载配置文件 | ohs.command.reload |