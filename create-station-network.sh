docker network create --gateway=172.19.0.1 --subnet=172.19.0.0/16 --attachable=true \
  --opt com.docker.network.bridge.default_bridge=true \
  --opt com.docker.network.bridge.enable_icc=true \
  --opt com.docker.network.bridge.enable_ip_masquerade=true \
  --opt com.docker.network.bridge.host_binding_ipv4=0.0.0.0 \
  --opt com.docker.network.bridge.name=docker0 \
  --opt com.docker.network.driver.mtu=1500 \
  station