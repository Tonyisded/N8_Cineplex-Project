'use strict';
(() => {
  const channel='cineplex-seat-preview-v1';
  const frame=document.getElementById('customer-frame');
  const origin=location.origin==='null'?'*':location.origin;
  document.querySelectorAll('[data-scenario]').forEach(button=>button.addEventListener('click',()=>{
    frame.contentWindow.postMessage({channel,type:'scenario',action:button.dataset.scenario},origin);
    document.getElementById('preview-message').textContent='Đã gửi tình huống: '+button.textContent+'.';
  }));
  let lastMessage='';
  window.addEventListener('message',event=>{
    if(event.source!==frame.contentWindow||event.origin!==location.origin||event.data?.channel!==channel||event.data?.type!=='state')return;
    const s=event.data.payload;
    document.getElementById('connection-state').textContent=!s.online?'Mất kết nối':s.syncing?'Đang đồng bộ':s.pending?'Đang giữ ghế':'Đã kết nối';
    document.getElementById('remaining-state').textContent=s.remaining?`${String(Math.floor(s.remaining/60)).padStart(2,'0')}:${String(s.remaining%60).padStart(2,'0')}`:'Chưa giữ / đã hết';
    document.getElementById('places-state').textContent=s.places;
    document.getElementById('total-state').textContent=new Intl.NumberFormat('vi-VN').format(s.total)+'đ';
    document.getElementById('selection-state').textContent=s.selected.length?'Đang giữ: '+s.selected.map(id=>id.replace('-', '–')).join(', '):'Chưa chọn ghế';
    if(s.lastMessage&&s.lastMessage!==lastMessage){lastMessage=s.lastMessage;document.getElementById('preview-message').textContent=s.lastMessage;}
  });
})();
