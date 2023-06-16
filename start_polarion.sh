#Initialize psql
/opt/polarion/bin/postgresql-polarion.init restart

#Initialize apache
service apache2 restart

#Initialize Polarion
/opt/polarion/bin/polarion.init start
