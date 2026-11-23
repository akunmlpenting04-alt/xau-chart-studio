const assert=require('node:assert/strict');
const {handler}=require('../netlify/functions/candles.js');
const evt=(tf='M5',token='',method='GET',origin='https://appassets.androidplatform.net')=>({httpMethod:method,queryStringParameters:{tf},headers:{origin,'x-xau-access-token':token}});
(async()=>{
 delete process.env.TWELVE_DATA_API_KEY;delete process.env.XAU_APP_ACCESS_TOKEN;
 assert.equal((await handler(evt())).statusCode,503);
 process.env.TWELVE_DATA_API_KEY='fake-td-key-for-tests';
 process.env.XAU_APP_ACCESS_TOKEN='fake-app-access-token-123';
 assert.equal((await handler(evt('M5','no'))).statusCode,401);
 assert.equal((await handler(evt('INVALID','fake-app-access-token-123'))).statusCode,400);
 assert.equal((await handler(evt('M5','','OPTIONS'))).statusCode,204);
 assert.equal((await handler(evt('M5','fake-app-access-token-123','GET','https://evil.example'))).statusCode,403);
 let hits=0;
 global.fetch=async(url,params)=>{
  hits++;
  assert.equal(url.searchParams.get('symbol'),'XAU/USD');
  assert.equal(url.searchParams.has('apikey'),false);
  assert.equal(params.headers.Authorization,'apikey fake-td-key-for-tests');
  return {ok:true,status:200,json:async()=>({values:[{datetime:'2026-11-23 14:30:00',open:'4002',high:'4005',low:'4000',close:'4004'},{datetime:'2026-11-23 14:25:00',open:'4000',high:'4004',low:'3998',close:'4002'}]})};
 };
 const result=await handler(evt('M5','fake-app-access-token-123'));
 assert.equal(result.statusCode,200);assert.equal(JSON.parse(result.body).bars.length,2);
 await handler(evt('M5','fake-app-access-token-123'));assert.equal(hits,1);
 console.log('Netlify backend tests passed');
})().catch(e=>{console.error(e);process.exitCode=1});
