// Netlify adapter for XAU/USD; Twelve Data API key is never sent to client.
const crypto=require('node:crypto');
const INTERVAL={M1:'1min',M5:'5min',M15:'15min',M30:'30min',H1:'1h',H4:'4h',D1:'1day'};
const cache=new Map();
const respond=(statusCode,payload,h={})=>({statusCode,headers:{'Content-Type':'application/json; charset=utf-8','Cache-Control':'private, no-store',...h},body:JSON.stringify(payload)});
const validToken=(a,b)=>{const aa=Buffer.from(a||'','utf8'),bb=Buffer.from(b||'','utf8');return aa.length>0&&aa.length===bb.length&&crypto.timingSafeEqual(aa,bb)};
function cors(event){
 const origin=String(event.headers?.origin||event.headers?.Origin||'');
 if(!origin)return {};
 const allowed=['https://appassets.androidplatform.net',process.env.URL,process.env.DEPLOY_PRIME_URL].filter(Boolean);
 if(!allowed.includes(origin))return null;
 return {'Access-Control-Allow-Origin':origin,'Access-Control-Allow-Headers':'X-XAU-Access-Token','Access-Control-Allow-Methods':'GET, OPTIONS','Access-Control-Max-Age':'600','Vary':'Origin'};
}
function parseDate(raw){
 const s=String(raw||'').trim(),t=/^\d{4}-\d{2}-\d{2}$/.test(s)?s+'T00:00:00Z':s.replace(' ','T')+'Z';
 const n=Math.floor(new Date(t).getTime()/1000);return Number.isFinite(n)?n:0;
}
exports.handler=async(event)=>{
 const c=cors(event);
 if(c===null)return respond(403,{error:'Origin tidak diizinkan'});
 if(event.httpMethod==='OPTIONS')return {statusCode:204,headers:c,body:''};
 if(event.httpMethod!=='GET')return respond(405,{error:'Hanya GET'},c);
 const tf=String(event.queryStringParameters?.tf||'M5').toUpperCase();
 if(!INTERVAL[tf])return respond(400,{error:'Timeframe tidak valid'},c);
 const key=process.env.TWELVE_DATA_API_KEY||'';
 const token=process.env.XAU_APP_ACCESS_TOKEN||'';
 if(!key||!token)return respond(503,{error:'Netlify belum siap. Simpan TWELVE_DATA_API_KEY dan XAU_APP_ACCESS_TOKEN dengan scope Functions, lalu deploy ulang.'},c);
 const supplied=event.headers?.['x-xau-access-token']||event.headers?.['X-XAU-Access-Token']||'';
 if(!validToken(String(supplied),token))return respond(401,{error:'Kode akses aplikasi salah atau belum diisi.'},c);
 const hit=cache.get(tf);
 if(hit&&Date.now()-hit.at<65000)return respond(200,hit.data,c);
 const controller=new AbortController(),timeout=setTimeout(()=>controller.abort(),13000);
 try{
  const url=new URL('https://api.twelvedata.com/time_series');
  url.searchParams.set('symbol','XAU/USD');
  url.searchParams.set('interval',INTERVAL[tf]);
  url.searchParams.set('timezone','UTC');
  url.searchParams.set('outputsize','500');
  const r=await fetch(url,{signal:controller.signal,headers:{Authorization:'apikey '+key,Accept:'application/json'}});
  const data=await r.json();
  if(!r.ok||data.status==='error'||!Array.isArray(data.values)){
   const msg=String(data.message||'Paket, akses simbol, atau kuota perlu diperiksa').replaceAll(key,'[redacted]').slice(0,160);
   throw Error('Twelve Data: '+msg);
  }
  const raw=data.values.map(v=>({time:parseDate(v.datetime),open:Number(v.open),high:Number(v.high),low:Number(v.low),close:Number(v.close)}))
   .filter(v=>v.time>1000000000&&[v.open,v.high,v.low,v.close].every(Number.isFinite)&&v.high>=Math.max(v.open,v.close)&&v.low<=Math.min(v.open,v.close))
   .sort((a,b)=>a.time-b.time);
  const bars=raw.filter((v,i)=>i===0||v.time!==raw[i-1].time);
  if(bars.length<2)throw Error('Data candle dari Twelve Data tidak cukup');
  const body={symbol:'XAU/USD',timeframe:tf,provider:'Twelve Data',timezone:'UTC',barCount:bars.length,updatedAt:new Date().toISOString(),bars};
  cache.set(tf,{at:Date.now(),data:body});
  return respond(200,body,c);
 }catch(e){
  const error=e.name==='AbortError'?'Timeout saat menghubungi Twelve Data':String(e.message||'Gagal mengambil candle').replaceAll(key,'[redacted]').slice(0,200);
  return respond(502,{error},c);
 }finally{clearTimeout(timeout)}
};