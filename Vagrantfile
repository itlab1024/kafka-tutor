Vagrant.configure("2") do |config|
   (1..3).each do |i|
        config.vm.define "kraft#{i}" do |node|
            # 设置虚拟机的Box。指定本地的box文件
            node.vm.box = "boxomatic/centos-stream-9"

            # 设置虚拟机的主机名
            node.vm.hostname="kraft#{i}"

            # 设置虚拟机的IP
            node.vm.network "private_network", ip: "192.168.10.1#{i}"

            # VirtualBox相关配置
            node.vm.provider "virtualbox" do |v|
                # 设置虚拟机的名称
                v.name = "kraft#{i}"
                # 设置虚拟机的内存大小
                v.memory = 2048
                # 设置虚拟机的CPU个数
                v.cpus = 1
            end
        end
   end
end
