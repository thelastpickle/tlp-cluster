FROM alpine:3.10.2

COPY parallel_ssh.sh /usr/local/bin/
COPY copy_provisioning_resources.sh /usr/local/bin/

RUN apk --no-cache add pssh rsync curl jq openssh-client && \
 mkdir /tlp && \
 mkdir /root/.ssh && \
 chmod +x /usr/local/bin/*.sh

WORKDIR /local