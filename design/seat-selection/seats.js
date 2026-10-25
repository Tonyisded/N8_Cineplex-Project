/* Cineplex SeatSelection: local, deterministic demonstration. No inventory API. */
(() => {
  'use strict';
  const CHANNEL = 'cineplex-seat-preview-v1';
  const PREVIEW = new URLSearchParams(location.search).get('preview') === '1' && window.parent !== window;
  const LABEL = {standard:'Thường', vip:'VIP', couple:'Couple'};
  const PRICE = {standard:90000, vip:120000, couple:240000};
  const STATUS = {available:'Còn trống', held:'Người khác đang giữ', sold:'Đã bán'};
  const money = amount => new Intl.NumberFormat('vi-VN').format(amount) + 'đ';
  const escape = text => String(text).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const S = {seats:[], selected:new Set(), pending:new Set(), snapshot:new Map(), deadline:0, expired:false, online:true, syncing:false, confirming:false, generation:0, networkGeneration:0, zoom:1, key:'', failNext:new Set(), leaveTarget:null, lastMessage:'', announceUrgent:false};
  let kit = null, ticker = null, mapGesture = null, mapLeft = 0, mapTop = 0;
  function createSeats() {
    const sold = new Set(['A1','A6','B2','C4','D3','G5']);
    const held = new Set(['E2','H3','I3-I4']);
    S.seats = [];
    for (const row of 'ABCDEFGHIJ') {
      const type = row < 'E' ? 'standard' : row < 'I' ? 'vip' : 'couple';
      for (let number=1; number<=6; number+=type==='couple'?2:1) {
        const codes = type==='couple' ? [`${row}${number}`,`${row}${number+1}`] : [`${row}${number}`];
        const id = codes.join('-');
        S.seats.push({id,codes,row,type,price:PRICE[type],places:codes.length,status:sold.has(id)?'sold':held.has(id)?'held':'available'});
      }
    }
    S.snapshot = new Map(S.seats.map(seat=>[seat.id,seat.status]));
  }
  createSeats();
  const isSeatRoute = () => ['seat-handoff','checkout-handoff'].includes(kit?.state.route);
  const left = () => S.deadline ? Math.max(0,Math.ceil((S.deadline-Date.now())/1000)) : 0;
  const timeText = seconds => `${String(Math.floor(seconds/60)).padStart(2,'0')}:${String(seconds%60).padStart(2,'0')}`;
  const selectedSeats = (pending=false) => S.seats.filter(seat=>S.selected.has(seat.id)||(pending&&S.pending.has(seat.id)));
  const total = (pending=false) => selectedSeats(pending).reduce((sum,seat)=>sum+seat.price,0);
  const capacity = () => selectedSeats(true).reduce((sum,seat)=>sum+seat.places,0);
  const seatName = seat => seat.type==='couple' ? `${seat.codes[0]}–${seat.codes[1]}` : seat.id;
  const lockIcon = () => '<svg class="icon" viewBox="0 0 24 24" aria-hidden="true"><rect x="5" y="10" width="14" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3m-4 5v2"/></svg>';
  const wifiIcon = () => '<svg class="icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M3 8a15 15 0 0 1 18 0M6 12a10 10 0 0 1 12 0m-9 4a5 5 0 0 1 6 0m-3 4h.01M3 3l18 18"/></svg>';
  const zoomIcon = name => `<svg class="icon" viewBox="0 0 24 24" aria-hidden="true">${name==='reset'?'<path d="M3 10a9 9 0 1 1 2 8M3 4v6h6"/>':`<path d="M5 12h14${name==='plus'?'m-7-7v14':''}"/>`}</svg>`;
  function actionButton(label, action, variant='primary', extra='') {
    return `<button type="button" class="btn btn-${variant}" data-component="CineplexButton" data-seat-action="${action}" ${extra}>${label}</button>`;
  }
  function ensureContext() {
    if (!kit.state.selected) {
      const cinema = kit.cinemas.find(c=>c.id==='dongkhoi')||kit.cinemas[0];
      kit.state.date = 16;
      kit.state.selected = {id:`${cinema.id}-2D · Phụ đề-19:30`,cinema,format:'2D · Phụ đề',time:'19:30'};
    }
    const booking = kit.state.selected;
    const key = `${booking.id}-${kit.state.date}`;
    if (S.key && S.key !== key) { release(); createSeats(); }
    S.key = key;
    return booking;
  }
  function header(title='Chọn ghế', backAction='back') {
    return `<header class="app-header"><button type="button" class="icon-button" data-seat-action="${backAction}" aria-label="Quay lại">${kit.Icon('back')}</button><h2>${title}</h2><span class="seat-step"><b>2</b><span>Chọn chỗ</span></span></header>`;
  }
  function bookingSummary() {
    const booking = ensureContext();
    return `<section class="seat-booking" data-component="SeatBookingSummary"><span class="poster-small"><img src="assets/poster.jpg" width="780" height="1170" alt="Poster Dune: Hành tinh cát – Phần hai"></span><div class="od-field od-fill"><h1>${kit.film.name}</h1><div class="seat-booking-line">${kit.AgeBadge()}<span>${escape(booking.cinema.name)} · Phòng 03</span></div><div class="seat-booking-line"><strong class="od-nowrap">${booking.time} · ${kit.state.date}/10/2026</strong><span>${escape(booking.format)}</span></div></div></section>`;
  }
  function countdown() {
    return `<div id="hold-countdown" class="hold-strip${!S.deadline?' is-empty':''}" data-component="HoldCountdown">${kit.Icon('clock')}<div class="od-field"><strong id="hold-title">Thời gian giữ ghế</strong><p id="hold-helper">Ghế được giữ trong 10 phút từ khi bạn chọn ghế đầu tiên</p></div><span id="hold-time" class="hold-time" aria-label="Thời gian giữ ghế còn lại">10:00</span></div>`;
  }
  function connectionBanner() {return '<div id="seat-connection" class="seat-connection" data-component="ConnectionBanner" role="status" aria-live="polite" hidden></div>';}
  function seatButton(seat) {
    const selected=S.selected.has(seat.id),pending=S.pending.has(seat.id);
    const status=pending?'pending':selected?'selected':seat.status;
    const stateLabel=pending?'Đang xác nhận giữ ghế':selected?'Đang được bạn chọn':STATUS[seat.status];
    const label=`${LABEL[seat.type]} ${seatName(seat)}, ${seat.places} chỗ, ${money(seat.price)}${seat.type==='couple'?' mỗi cặp':''}, ${stateLabel}`;
    const mark=pending?'…':selected?'✓':seat.status==='held'?lockIcon():seat.status==='sold'?'×':seat.type==='vip'?'V':'+';
    const glyph=seat.type==='couple'?`<span class="couple-codes"><span>${seat.codes[0]}</span><span>${seat.codes[1]}</span></span><span class="seat-mark">${pending?'Đang giữ':selected?'✓ 2 chỗ':seat.status==='held'?lockIcon():seat.status==='sold'?'×':'2 chỗ'}</span>`:`<span class="seat-code">${seat.id}</span><span class="seat-mark">${mark}</span>`;
    return `<button type="button" class="seat${seat.type==='couple'?' seat-couple':''}${seat.type==='vip'?' is-vip':''} is-${status}${!S.online||S.syncing?' is-locked':''}" data-component="${seat.type==='couple'?'CoupleSeat':'SingleSeat'}" data-seat-id="${seat.id}" aria-label="${escape(label)}" aria-pressed="${selected}" aria-disabled="${!S.online||S.syncing||S.confirming||pending||['held','sold'].includes(seat.status)}"><span class="seat-glyph">${glyph}</span></button>`;
  }
  function mapRows() {
    return [...'ABCDEFGHIJ'].map(row=>{
      const seats=S.seats.filter(seat=>seat.row===row),couple=seats[0].type==='couple';
      return `<div class="seat-row${couple?' couple-row':''}${row==='E'||row==='I'?' section-start':''}" role="group" aria-label="Hàng ${row}, ${LABEL[seats[0].type]}"><span class="row-label">${row}</span>${seats.map((seat,index)=>`${!couple&&index===3?'<span class="aisle" aria-hidden="true"></span>':''}${seatButton(seat)}`).join('')}</div>`;
    }).join('');
  }
  function mapView() {
    return `<section class="map-section" aria-label="Sơ đồ ghế"><div class="map-toolbar"><h2>Chọn chỗ bạn thích</h2><div class="map-tools"><button type="button" class="icon-button" data-seat-action="zoom-out" aria-label="Thu nhỏ sơ đồ">${zoomIcon('minus')}</button><span id="zoom-value" class="zoom-value">100%</span><button type="button" class="icon-button" data-seat-action="zoom-in" aria-label="Phóng to sơ đồ">${zoomIcon('plus')}</button><button type="button" class="icon-button" data-seat-action="reset-map" aria-label="Đưa sơ đồ về vị trí ban đầu">${zoomIcon('reset')}</button></div></div><p class="map-hint">Kéo hoặc cuộn để xem toàn bộ ghế.</p><div id="seat-map-viewport" class="map-viewport" tabindex="0" aria-label="Sơ đồ ghế cuộn được; dùng phím mũi tên để di chuyển"><div class="seat-canvas" id="seat-canvas" style="--seat-zoom:${S.zoom}"><div class="cinema-screen"><svg viewBox="0 0 280 20" fill="none" aria-hidden="true"><path d="M4 16Q140 -6 276 16" stroke="#8c2945" stroke-width="3" stroke-linecap="round"/></svg><span>MÀN HÌNH</span></div><div class="seat-row number-row" aria-hidden="true"><span></span><span>1</span><span>2</span><span>3</span><span></span><span>4</span><span>5</span><span>6</span></div><div id="seat-rows">${mapRows()}</div></div></div><p id="seat-live-status" class="seat-live-status"><span class="seat-live-dot"></span><span>Trạng thái ghế được cập nhật</span></p></section>`;
  }
  function legend() {
    return `<section class="seat-legend" data-component="SeatLegend"><h2>Chú thích ghế</h2><div class="legend-grid"><div class="legend-item"><span class="legend-seat">+</span><span>Còn trống</span></div><div class="legend-item"><span class="legend-seat selected">✓</span><span>Đang chọn</span></div><div class="legend-item"><span class="legend-seat held">${lockIcon()}</span><span>Người khác giữ</span></div><div class="legend-item"><span class="legend-seat sold">×</span><span>Đã bán</span></div></div></section><section class="seat-price-list" aria-label="Giá vé mẫu"><div class="od-field"><strong>Thường</strong><span>90.000đ/chỗ</span></div><div class="od-field"><strong>VIP · Ký hiệu V</strong><span>120.000đ/chỗ</span></div><div class="od-field couple-price"><strong>Couple · Ghế đôi liền nhau</strong><span>240.000đ/cặp · Gồm 2 chỗ</span></div></section>`;
  }
  function selectedList() {
    const seats=selectedSeats(true);
    if(!seats.length)return '<div class="seat-empty-choice"><strong>Chưa chọn ghế</strong><span>Chọn một chỗ để bắt đầu</span></div>';
    return `<div class="od-rail seat-selection-list" aria-label="Ghế đã chọn; cuộn ngang để xem đầy đủ">${seats.map(seat=>`<button type="button" class="selected-seat-tag" data-seat-id="${seat.id}" aria-label="Bỏ chọn ${LABEL[seat.type]} ${seatName(seat)}"><span class="od-field"><strong>${LABEL[seat.type]} ${seatName(seat)} · ${seat.places} chỗ</strong><small>${S.pending.has(seat.id)?'Đang xác nhận giữ ghế…':money(seat.price)+(seat.type==='couple'?'/cặp':'/chỗ')}</small></span>${kit.Icon('close',true)}</button>`).join('')}</div>`;
  }
  function footer() {
    return `<footer class="action-bar seat-footer" data-component="SeatSummaryBar"><div id="selected-seat-list">${selectedList()}</div><span id="seat-footer-status" class="seat-footer-status" role="status"></span><div class="seat-footer-bottom"><div class="od-stat seat-total"><span id="seat-total-label" class="total-label">Tổng tiền · ${capacity()} chỗ</span><strong id="seat-total">${money(total(true))}</strong></div>${actionButton(`Tiếp tục ${kit.Icon('arrow',true)}`,'continue','primary','id="seat-continue" disabled')}</div></footer>`;
  }
  function view() {
    ensureContext();
    return `${header()}<div class="od-scroll" id="page-scroll">${bookingSummary()}<div class="seat-status-group">${countdown()}${connectionBanner()}</div>${mapView()}${legend()}</div>${footer()}`;
  }
  function checkoutView() {
    if(!canCheckout()){kit.state.route='seat-handoff';return view();}
    const b=ensureContext();
    return `${header('Bắp nước & thanh toán','back-to-seats')}<div class="od-scroll" id="page-scroll">${countdown()}<section class="handoff seat-checkout"><div class="empty-icon">${kit.Icon('check')}</div><h1>Ghế đã sẵn sàng</h1><p class="muted">Bạn có thể tiếp tục chọn bắp nước và hoàn tất thanh toán ở bước tiếp theo.</p><div class="booking-summary"><h2>${kit.film.name}</h2><div class="od-cluster">${kit.AgeBadge()}<span class="small muted">Phòng 03</span></div><p>${escape(b.cinema.name)}<br><strong>${b.time} · ${kit.state.date}/10/2026</strong><br>${escape(b.format)}</p><div class="seat-checkout-list">${selectedSeats().map(seat=>`<div class="od-row"><span class="od-fill">${LABEL[seat.type]} ${seatName(seat)} · ${seat.places} chỗ</span><strong class="od-nowrap">${money(seat.price)}</strong></div>`).join('')}</div><div class="seat-checkout-total"><span>Tổng tiền</span><strong>${money(total())}</strong></div></div><p class="small muted">Điểm bàn giao của prototype. Chưa thực hiện giữ ghế hoặc thanh toán thật.</p></section></div><footer class="action-bar">${actionButton('Quay lại chọn ghế','back-to-seats','secondary')}</footer>`;
  }
  function sheetBody(type) {
    if(type==='seat-expired')return `<div class="age-emblem warning">${kit.Icon('clock')}</div><h3 id="sheet-title">Đã hết thời gian giữ ghế</h3><p>Các ghế bạn chọn đã được giải phóng. Vui lòng chọn lại để tiếp tục.</p><div class="sheet-actions">${actionButton('Chọn lại ghế','retry')}${actionButton('Về lịch chiếu','expired-showtimes','secondary')}</div>`;
    if(type==='seat-leave')return `<div class="age-emblem warning">${kit.Icon('ticket')}</div><h3 id="sheet-title">Rời màn hình chọn ghế?</h3><p>Bạn muốn rời màn hình chọn ghế? Các ghế đang giữ sẽ được giải phóng.</p><div class="sheet-actions">${actionButton('Ở lại','stay')}${actionButton('Rời màn hình','leave','secondary')}</div>`;
    return '';
  }
  function notify(message) { S.lastMessage=message;kit.toast(message);sendStatus(); }
  function sendStatus() {
    if(!PREVIEW||!kit)return;
    parent.postMessage({channel:CHANNEL,type:'state',payload:{online:S.online,syncing:S.syncing,pending:S.pending.size,selected:selectedSeats().map(s=>s.id),places:capacity(),total:total(true),remaining:left(),lastMessage:S.lastMessage,route:kit.state.route}},location.origin==='null'?'*':location.origin);
  }
  function canCheckout() {return S.online&&!S.syncing&&!S.confirming&&!S.pending.size&&S.selected.size>0&&left()>0&&selectedSeats().every(seat=>seat.status==='available');}
  function setBanner() {
    const banner=document.getElementById('seat-connection');
    if(!banner)return;
    const message=!S.online?'Mất kết nối. Đang kết nối lại…':S.syncing?'Đang cập nhật ghế…':'';
    if(banner.dataset.message===message)return;
    banner.dataset.message=message;banner.hidden=!message;banner.classList.toggle('is-updating',S.online);
    banner.innerHTML=message?`${S.online?kit.Icon('clock',true):wifiIcon()}<span>${message}</span>`:'';
  }
  function updateCountdown() {
    const root=document.getElementById('hold-countdown');if(!root)return;
    const seconds=left(),urgent=S.deadline>0&&seconds<120;
    root.classList.toggle('is-empty',!S.deadline&&!S.expired);root.classList.toggle('is-urgent',urgent);root.classList.toggle('is-expired',S.expired);
    const title=document.getElementById('hold-title'),helper=document.getElementById('hold-helper'),clock=document.getElementById('hold-time');
    clock.textContent=S.expired?'00:00':timeText(seconds||600);clock.style.display=S.deadline||S.expired?'block':'none';
    title.textContent=S.expired?'Đã hết thời gian giữ ghế':S.deadline?(urgent?'Thời gian giữ ghế sắp hết':'Thời gian giữ ghế'):'Giữ chỗ trong 10 phút';
    helper.textContent=S.expired?'Vui lòng chọn lại ghế để tiếp tục.':S.deadline?'Thêm hoặc đổi ghế không gia hạn thời gian.':'Ghế được giữ trong 10 phút từ khi bạn chọn ghế đầu tiên';
  }
  function updateUI(replaceSeats=true) {
    if(!kit)return;
    if(kit.state.route==='seat-handoff'){
      const focused=document.activeElement?.dataset?.seatId;
      if(replaceSeats){const rows=document.getElementById('seat-rows');if(rows)rows.innerHTML=mapRows();}
      const list=document.getElementById('selected-seat-list');
      if(list){const rail=list.querySelector('.seat-selection-list'),scroll=rail?.scrollLeft||0;list.innerHTML=selectedList();const fresh=list.querySelector('.seat-selection-list');if(fresh)fresh.scrollLeft=scroll;}
      const totalNode=document.getElementById('seat-total');if(totalNode)totalNode.textContent=money(total(true));
      const totalLabel=document.getElementById('seat-total-label');if(totalLabel)totalLabel.textContent=`Tổng tiền · ${capacity()} chỗ`;
      const button=document.getElementById('seat-continue');
      if(button){button.disabled=!canCheckout();button.setAttribute('aria-busy',String(S.confirming||S.pending.size>0));button.innerHTML=S.confirming?'Đang xác nhận…':S.pending.size?'Đang giữ ghế…':`Tiếp tục ${kit.Icon('arrow',true)}`;}
      const status=document.getElementById('seat-footer-status');if(status)status.textContent=!S.online?'Kết nối lại để tiếp tục.':S.syncing?'Đang kiểm tra lại ghế đã chọn.':S.pending.size?'Đang xác nhận giữ ghế…':'';
      const live=document.getElementById('seat-live-status');if(live){live.classList.toggle('is-offline',!S.online);live.lastElementChild.textContent=!S.online?'Đang chờ kết nối lại':S.syncing?'Đang cập nhật ghế…':'Trạng thái ghế được cập nhật';}
      if(focused&&replaceSeats&&!kit.state.modal)document.querySelector(`#seat-rows [data-seat-id="${focused}"]`)?.focus({preventScroll:true});
    }
    setBanner();updateCountdown();sendStatus();
  }
  function tick() {
    if(!kit)return;
    if(S.deadline&&Date.now()>=S.deadline){expire();return;}
    if(S.deadline&&left()<120&&!S.announceUrgent){S.announceUrgent=true;if(isSeatRoute())notify('Thời gian giữ ghế sắp hết');}
    updateCountdown();
    const button=document.getElementById('seat-continue');if(button&&!S.confirming)button.disabled=!canCheckout();
    sendStatus();
  }
  function expire() {
    if(!S.deadline)return;
    S.generation++;S.selected.clear();S.pending.clear();S.confirming=false;S.deadline=0;S.expired=true;S.announceUrgent=false;
    if(isSeatRoute()){
      if(kit.state.route==='checkout-handoff')kit.navigate('seat-handoff',true);
      updateUI();kit.openSheet('seat-expired');
    }
    S.lastMessage='Đã hết thời gian giữ ghế';sendStatus();
  }
  function release() {
    S.generation++;S.pending.clear();S.selected.clear();S.deadline=0;S.expired=false;S.confirming=false;S.announceUrgent=false;S.failNext.clear();S.leaveTarget=null;
  }
  function selectSeat(id) {
    if(!kit||kit.state.route!=='seat-handoff')return;
    if(!S.online){notify('Mất kết nối. Vui lòng chờ kết nối lại để chọn ghế.');return;}
    if(S.syncing||S.confirming){notify('Đang cập nhật ghế. Vui lòng đợi trong giây lát.');return;}
    if(S.deadline&&left()===0){expire();return;}
    const seat=S.seats.find(s=>s.id===id);if(!seat)return;
    if(seat.status!=='available'){notify(seat.status==='sold'?`Ghế ${seatName(seat)} đã bán. Vui lòng chọn ghế khác.`:`Ghế ${seatName(seat)} đang được người khác giữ. Vui lòng chọn ghế khác.`);return;}
    if(S.pending.size){notify('Đang xác nhận giữ ghế. Vui lòng đợi trong giây lát.');return;}
    if(S.selected.has(id)){S.selected.delete(id);updateUI();notify(`Đã bỏ chọn ${LABEL[seat.type]} ${seatName(seat)}${seat.type==='couple'?' · cả 2 chỗ':''}`);return;}
    const generation=S.generation;
    S.pending.add(id);S.expired=false;updateUI();
    setTimeout(()=>{
      if(generation!==S.generation||!S.pending.has(id))return;
      S.pending.delete(id);
      if(!S.online){updateUI();return;}
      const conflict=S.failNext.has(id)||S.snapshot.get(id)!=='available';
      if(conflict){S.failNext.delete(id);seat.status=S.snapshot.get(id)==='sold'?'sold':'held';S.snapshot.set(id,seat.status);S.selected.delete(id);updateUI();notify(`Ghế ${seatName(seat)} vừa được người khác giữ. Vui lòng chọn ghế khác`);return;}
      if(S.deadline&&left()===0){expire();return;}
      S.selected.add(id);
      if(!S.deadline){S.deadline=Date.now()+600000;S.announceUrgent=false;}
      updateUI();notify(`Đã giữ ${LABEL[seat.type]} ${seatName(seat)} · ${seat.places} chỗ`);
    },650);
  }
  function remoteChange(id,status,quiet=false) {
    const seat=S.seats.find(s=>s.id===id);if(!seat)return;
    S.snapshot.set(id,status);
    if(!S.online){S.lastMessage='Có cập nhật đang chờ đồng bộ khi kết nối lại.';sendStatus();return;}
    seat.status=status;
    const removed=status!=='available'&&S.selected.delete(id);
    updateUI();
    if(!quiet)notify(removed?`Ghế ${seatName(seat)} không còn khả dụng. Lựa chọn và tổng tiền đã được cập nhật.`:status==='available'?`Ghế ${seatName(seat)} đã được giải phóng.`:`Ghế ${seatName(seat)} ${status==='sold'?'đã được bán':'đang được người khác giữ'}.`);
  }
  function offline() {
    S.online=false;S.syncing=false;S.networkGeneration++;S.generation++;S.pending.clear();S.confirming=false;
    updateUI();notify('Mất kết nối. Đang kết nối lại…');
  }
  function reconnect(change=false) {
    if(change){const id=selectedSeats()[0]?.id||'F6';S.snapshot.set(id,'held');}
    const network=++S.networkGeneration;
    S.online=true;S.syncing=true;updateUI();
    setTimeout(()=>{
      if(network!==S.networkGeneration||!S.online)return;
      const removed=[];
      for(const seat of S.seats){seat.status=S.snapshot.get(seat.id)||'available';if(S.selected.has(seat.id)&&seat.status!=='available'){S.selected.delete(seat.id);removed.push(seatName(seat));}}
      S.syncing=false;
      if(S.deadline&&left()===0){expire();return;}
      updateUI();notify(removed.length?`Đã kết nối lại. Ghế ${removed.join(', ')} không còn khả dụng; lựa chọn và tổng tiền đã cập nhật.`:'Đã kết nối lại. Ghế đã chọn và thời gian giữ đã được đồng bộ.');
    },900);
  }
  function updating() {
    if(!S.online)return;
    S.syncing=true;const network=++S.networkGeneration;updateUI();
    setTimeout(()=>{if(network!==S.networkGeneration||!S.online)return;S.syncing=false;updateUI();notify('Đã cập nhật trạng thái ghế.');},1100);
  }
  function confirm() {
    if(!canCheckout()){tick();notify('Vui lòng chọn ghế hợp lệ và kiểm tra kết nối để tiếp tục.');return;}
    const generation=S.generation;
    S.confirming=true;updateUI();
    setTimeout(()=>{
      if(generation!==S.generation)return;
      S.confirming=false;
      if(!canCheckout()){updateUI();if(S.deadline&&left()===0)expire();else notify('Ghế đã thay đổi. Vui lòng kiểm tra lựa chọn và thử lại.');return;}
      kit.navigate('checkout-handoff',true);
    },800);
  }
  function guard(target) {
    if(!isSeatRoute()||['seat-handoff','checkout-handoff'].includes(target))return false;
    if(S.selected.size||S.pending.size||S.confirming){S.leaveTarget=target;kit.openSheet('seat-leave');return true;}
    release();return false;
  }
  function changeZoom(zoom,reset=false) {
    const viewport=document.getElementById('seat-map-viewport'),canvas=document.getElementById('seat-canvas');if(!viewport||!canvas)return;
    const old=S.zoom,x=(viewport.scrollLeft+viewport.clientWidth/2)/old,y=(viewport.scrollTop+viewport.clientHeight/2)/old;
    S.zoom=Math.min(2,Math.max(1,zoom));canvas.style.setProperty('--seat-zoom',S.zoom);
    viewport.scrollLeft=reset?0:x*S.zoom-viewport.clientWidth/2;viewport.scrollTop=reset?0:y*S.zoom-viewport.clientHeight/2;
    mapLeft=viewport.scrollLeft;mapTop=viewport.scrollTop;
    document.getElementById('zoom-value').textContent=`${Math.round(S.zoom*100)}%`;
    document.querySelector('[data-seat-action="zoom-out"]').disabled=S.zoom<=1;
    document.querySelector('[data-seat-action="zoom-in"]').disabled=S.zoom>=2;
  }
  function bindMap() {
    const viewport=document.getElementById('seat-map-viewport');if(!viewport)return;
    const previousLeft=mapLeft,previousTop=mapTop;
    changeZoom(S.zoom);viewport.scrollLeft=previousLeft;viewport.scrollTop=previousTop;
    mapLeft=viewport.scrollLeft;mapTop=viewport.scrollTop;
    viewport.addEventListener('scroll',()=>{mapLeft=viewport.scrollLeft;mapTop=viewport.scrollTop;},{passive:true});
    viewport.addEventListener('pointerdown',event=>{
      if(event.pointerType!=='mouse'||event.button!==0)return;
      mapGesture={id:event.pointerId,x:event.clientX,y:event.clientY,left:viewport.scrollLeft,top:viewport.scrollTop,moved:false};
    });
    viewport.addEventListener('pointermove',event=>{
      if(!mapGesture||mapGesture.id!==event.pointerId||!(event.buttons&1))return;
      const dx=event.clientX-mapGesture.x,dy=event.clientY-mapGesture.y;
      if(Math.abs(dx)+Math.abs(dy)>6){mapGesture.moved=true;viewport.classList.add('is-dragging');viewport.setPointerCapture(event.pointerId);event.preventDefault();}
      if(mapGesture.moved){viewport.scrollLeft=mapGesture.left-dx;viewport.scrollTop=mapGesture.top-dy;}
    });
    viewport.addEventListener('pointerup',()=>{viewport.classList.remove('is-dragging');setTimeout(()=>{mapGesture=null;},0);});
    viewport.addEventListener('pointercancel',()=>{mapGesture=null;viewport.classList.remove('is-dragging');});
    viewport.addEventListener('click',event=>{if(mapGesture?.moved){event.preventDefault();event.stopImmediatePropagation();}},true);
    viewport.addEventListener('keydown',event=>{if(event.target!==viewport)return;const keys={ArrowLeft:[-52,0],ArrowRight:[52,0],ArrowUp:[0,-52],ArrowDown:[0,52]};if(keys[event.key]){event.preventDefault();viewport.scrollBy(...keys[event.key]);}});
  }
  function afterRender() {if(kit.state.route==='seat-handoff')bindMap();if(isSeatRoute()){updateUI(false);tick();}}
  function scenario(name) {
    if(!PREVIEW)return;
    if(!isSeatRoute())kit.navigate('seat-handoff',true);
    if(name==='hold')remoteChange('A2','held');
    else if(name==='sell')remoteChange('B4','sold');
    else if(name==='release'){remoteChange('A2','available',true);remoteChange('E2','available');}
    else if(name==='conflict'||name==='couple-conflict'){
      if(!S.online||S.syncing||S.pending.size||S.confirming){notify('Kết nối lại và chờ thao tác hiện tại kết thúc trước khi mô phỏng xung đột.');return;}
      if(kit.state.route!=='seat-handoff')kit.navigate('seat-handoff',true);
      const id=name==='conflict'?'F6':'J1-J2';S.selected.delete(id);const seat=S.seats.find(s=>s.id===id);seat.status='available';S.snapshot.set(id,'available');S.failNext.add(id);selectSeat(id);
    }
    else if(name==='updating')updating();
    else if(name==='offline')offline();
    else if(name==='reconnect')reconnect(false);
    else if(name==='reconnect-change')reconnect(true);
    else if(name==='urgent'||name==='expire'){
      if(!S.deadline){notify('Chọn và giữ thành công ít nhất một ghế trước khi mô phỏng thời gian.');return;}
      S.deadline=Date.now()+(name==='urgent'?119000:-1);S.announceUrgent=false;tick();updateUI(false);
    }
    else if(name==='reset'){
      release();S.networkGeneration++;S.online=true;S.syncing=false;S.zoom=1;mapLeft=0;mapTop=0;createSeats();kit.closeSheet(true);kit.navigate('seat-handoff',true);notify('Đã bắt đầu phiên mô phỏng mới.');
    }
  }
  document.addEventListener('click',event=>{
    if(!kit)return;
    const seat=event.target.closest('[data-seat-id]');if(seat){event.preventDefault();selectSeat(seat.dataset.seatId);return;}
    const button=event.target.closest('[data-seat-action]');if(!button||button.disabled)return;
    event.preventDefault();
    switch(button.dataset.seatAction){
      case 'back':kit.navigate('showtimes');break;
      case 'back-to-seats':kit.navigate('seat-handoff',true);break;
      case 'zoom-in':changeZoom(S.zoom+.25);break;
      case 'zoom-out':changeZoom(S.zoom-.25);break;
      case 'reset-map':changeZoom(1,true);break;
      case 'continue':confirm();break;
      case 'stay':S.leaveTarget=null;kit.closeSheet();break;
      case 'leave':{const target=S.leaveTarget||'showtimes';release();kit.closeSheet(true);kit.navigate(target,true);break;}
      case 'retry':kit.closeSheet(true);kit.navigate('seat-handoff',true);document.getElementById('seat-map-viewport')?.focus();break;
      case 'expired-showtimes':release();kit.closeSheet(true);kit.navigate('showtimes',true);break;
    }
  });
  window.addEventListener('message',event=>{
    if(!PREVIEW||event.source!==parent||event.origin!==location.origin||event.data?.channel!==CHANNEL||event.data?.type!=='scenario')return;
    const allowed=new Set(['hold','sell','release','conflict','couple-conflict','updating','offline','reconnect','reconnect-change','urgent','expire','reset']);
    if(allowed.has(event.data.action))scenario(event.data.action);
  });
  window.addEventListener('offline',()=>{if(kit)offline();});
  window.addEventListener('online',()=>{if(kit)reconnect();});
  window.addEventListener('beforeunload',event=>{if(S.selected.size||S.pending.size){event.preventDefault();event.returnValue='';}});
  document.addEventListener('visibilitychange',()=>{if(!document.hidden)tick();});
  window.CineplexSeats={view,checkoutView,sheetBody,guard,canCheckout,afterRender,init(context){kit=context;if(!ticker)ticker=setInterval(tick,1000);sendStatus();}};
})();
