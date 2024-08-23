#/bin/bash

doit() {
  token=`curl -s -X 'POST' \
    'http://localhost:8080/admin/register' \
    -H 'Content-Type: application/json' \
    -d '{
    "tenant": "tenant_a",
    "password": "tenant_a_secret",
    "realm": "heat",
    "subject": "Heat",
    "mode": "rwd",
    "expirationDate": "2099-12-31"
  }' | \
  python3 -c "import sys, json; print(json.load(sys.stdin)['token'])"`

  for i in $(seq 1 100);
  do
    curl -s \
      'http://localhost:8080/put?path=pictures%2Fcats&hash=true' \
      -H 'Content-Type: multipart/form-data' \
      -H "Authorization: Bearer ${token}" \
      -F 'file=@cat.jpg' > /dev/null

    curl -s -X 'GET' \
      'http://localhost:8080/list?path=pictures%2Fcats' \
      -H "Authorization: Bearer ${token}" > /dev/null

    curl -s -X 'GET' \
      'http://localhost:8080/get?file=pictures%2Fcats%2Fcat.jpg' \
      -H "Authorization: Bearer ${token}" \
      --output /dev/null > /dev/null
  done
}
export -f doit

for i in $(seq 1 100);
do
  doit &
done


