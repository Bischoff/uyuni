ARG BASE
ARG VERSION
FROM ${BASE}:${VERSION}

RUN zypper -n --gpg-auto-import-keys ref && \
      zypper -n install \
      java-17-openjdk-devel \
      rsync \
      apache-ivy \
      ant \
      ant-junit \
      servletapi5 \
      cpio \
      spacecmd &&
    zypper addrepo --no-gpgcheck https://download.opensuse.org/repositories/systemsmanagement:/Uyuni:/Utils/SLE_15_SP6/ systemsmanagement:uyuni:utils && \
    zypper addrepo --no-gpgcheck https://download.opensuse.org/repositories/systemsmanagement:/Uyuni:/Stable:/SLE15-Uyuni-Client-Tools/SLE_15/systemsmanagement:Uyuni:Stable:SLE15-Uyuni-Client-Tools.repo && \
    zypper -n install obs-to-maven yarn prometheus && \
    zypper clean -a
COPY minima.yaml /etc/minima.yaml
RUN mkdir /tmp/minima && \
    cd /tmp/minima && \
    wget -c https://github.com/moio/minima/releases/download/v0.10/minima-linux-amd64.tar.gz && \
    tar zxvf minima-linux-amd64.tar.gz && \
    cp minima /usr/bin/minima
RUN /usr/bin/minima sync -c /etc/minima.yaml && \
    mkdir /srv/www/htdocs/pub/TestRepoRpmUpdates /srv/www/htdocs/pub/TestRepoAppStream && \
    mv /srv/www/htdocs/pub/repositories/systemsmanagement\:/Uyuni\:/Test-Packages\:/Updates/rpm/* /srv/www/htdocs/pub/TestRepoRpmUpdates/ && \
    mv /srv/www/htdocs/pub/repositories/systemsmanagement\:/Uyuni\:/Test-Packages\:/Appstream/rhlike/* /srv/www/htdocs/pub/TestRepoAppStream/ && \
    rm -rf /srv/www/htdocs/pub/repositories/
RUN cd /srv/www/htdocs/pub && ln -s TestRepoRpmUpdates AnotherRepo
RUN mkdir /etc/pki/rpm-gpg && wget -c http://download.opensuse.org/repositories/systemsmanagement:/Uyuni:/Test-Packages:/Pool/rpm/repodata/repomd.xml.key -O  /etc/pki/rpm-gpg/uyuni-tools-gpg-pubkey-0d20833e.key
COPY mirror /mirror

