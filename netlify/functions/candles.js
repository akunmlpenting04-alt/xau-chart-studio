// Server-side-only Twelve Data API adapter. Keep TWELVE_DATA_API_KEY out of browser code.
const FRAME_TO_INTERVAL={M1:'1min',M5:'5min',M15:'15min',M30:'30min',H1:'1h',H4:'4h',D1:'1day'};
const memCache=new Map();
exports.handler=async(event)=>{
 const tf=String(event.queryStringParameters?.tf||'M5').toUpperCase();
 if(!FRAME_TO_INTERVAL[tf])return {statusCode:400,body:JSON.stringify({error:'Timeframe tidak valid'})};
 const key=process.env.TWELVE_DATA_API_KEY;
 if(!key)return {statusCode:503,headers:{'content-type':'application/json'},body:JSON.stringify({error:'Server belum memiliki TWELVE_DATA_API_KEY. Tambahkan di Netlify → Environment variables, lalu deploy ulang.'})};
 const cached=memCache.get(tf);
 if(cached&&Date.now()-cached.at<60000)return {statusCode:200,headers:{'content-type':'application/json','cache-control':'public,max-age=35'},body:JSON.stringify(cached.body)};
 try{
  const url=new URL('https://api.twelvedata.com/time_series');
  url.searchParams.set('symbol','XAU/USD');url.searchParams.set('interval',FRAME_TO_INTERVAL[tf]);url.searchParams.set('outputsize','500');url.searchParams.set('timezone','UTC');url.searchParams.set('apikey',key);
  const controller=new AbortController(),timeout=setTimeout(()=>controller.abort(),13000);
  const response=await fetch(url,{signal:controller.signal});clearTimeout(timeout);
  const data=await response.json();
  if(!response.ok||data.status==='error'||!Array.isArray(data.values))throw Error(data.message||`Twelve Data HTTP ${response.status}: tidak ada candle`);
  const bars=data.values.map(v=>({time:Math.floor(new Date(v.datetime.replace(' ','T')+'Z').getTime()/1000),open:Number(v.open),high:Number(v.high),low:Number(v.low),close:Number(v.close)})).filter(v=>Number.isFinite(v.time)&&v.time>1000000000&&[v.open,v.high,v.low,v.close].every(Number.isFinite)).sort((a,b)=>a.time-b.time);
  if(bars.length<2)throw Error('Data historis tidak cukup');
  const payload={symbol:'XAU/USD',timeframe:tf,provider:'Twelve Data',timezone:'UTC',barCount:bars.length,updatedAt:new Date().toISOString(),bars};
  memCache.set(tf,{at:Date.now(),body:payload});
  return {statusCode:200,headers:{'content-type':'application/json','cache-control':'public,max-age=35'},body:JSON.stringify(payload)};
 }catch(err){return {statusCode:502,headers:{'content-type':'application/json'},body:JSON.stringify({error:String(err.message||'Gagal mengambil data dari penyedia API').slice(0,240)})}}
};
