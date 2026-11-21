FROM alpine:3.21
RUN apk add --no-cache dnsmasq bind-tools && mkdir -p /var/lib/misc && touch /var/lib/misc/dnsmasq.leases
COPY dnsmasq.conf /etc/dnsmasq.conf
EXPOSE 53/tcp 53/udp 67/udp
CMD ["dnsmasq", "--keep-in-foreground", "--conf-file=/etc/dnsmasq.conf"]
