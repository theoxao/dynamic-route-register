# dynamic-route-register
dynamic add route to service using ktor and redis event notify

to enable redis event notify, edit `redis.conf` and change `notify-keyspace-events ""` to `notify-keyspace-events AE`.
then restart redis server
# this is only a demo
