# -*- mode: ruby -*-
# vi: set ft=ruby :

# Vagrantfile API/syntax version. Don't touch unless you know what you're doing!
VAGRANTFILE_API_VERSION = "2"

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  config.vm.box = "ubuntu/wily64"

  if Vagrant.has_plugin?("vagrant-cachier")
    config.cache.scope = :box
  end

  config.vm.network "forwarded_port", guest: 8000, host: 8000

  config.vm.provider "virtualbox" do |vb|
    vb.memory = 1024
    vb.cpus = 2
  end



  config.vm.provision "shell", inline: <<SCRIPT
    set -x
    set -e
    apt-get -y update

    # now prepare the build
    cd ..
    apt-get install -yqf \
        build-essential \
        git \
        python-dev \
        virtualenv \
        python3 \
        python3-virtualenv \
        python3-pip \
        python3-dev \
        python3.5 \
        python3.5-dev \
        python3.5-venv \
        libpq-dev \
        redis-server \
        postgresql-9.4 \
        postgresql-client-9.4
    easy_install pip

    echo "CREATE DATABASE qabel_drop; CREATE USER qabel WITH PASSWORD 'qabel_test'; GRANT ALL PRIVILEGES ON DATABASE qabel_drop TO qabel;" | sudo -u postgres psql postgres

    if [ ! -d /home/vagrant/.virtualenv ]; then
        mkdir /home/vagrant/.virtualenv
        echo -e "[virtualenv]\nalways-copy=true" > /home/vagrant/.virtualenv/virtualenv.ini
        chown -R vagrant:vagrant /home/vagrant/.virtualenv
    fi
    update-ca-certificates -f
SCRIPT


  config.vm.provision "shell", run: "always", privileged: "false", inline: <<SCRIPT
    set -e
    set -x
    cd /vagrant
    if [ ! -d qabel-block ]; then
        git clone https://github.com/Qabel/qabel-block
    fi
    cd qabel-block/src
    if [ ! -d ../venv ]; then
        virtualenv ../venv --no-site-packages --always-copy --python=python3.5
    fi
    source ../venv/bin/activate
    pip install -r ../requirements.txt
    nohup python run.py --port=8000 --address=0.0.0.0 --dummy --dummy-auth=MAGICFAIRY --dummy-log --debug > block.log 2>&1 &
SCRIPT
end
