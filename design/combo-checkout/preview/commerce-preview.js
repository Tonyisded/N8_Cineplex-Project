/* Controls belong to this preview document, outside the customer iframe. */
(() => {
  'use strict';
  const channel='cineplex-seat-preview-v1',frame=document.getElementById('customer-frame');
  const group=document.createElement('section');group.className='scenario-group';group.setAttribute('aria-label','Mô phỏng combo và thanh toán');
  const sections=[
    ['Ví dụ xuyên suốt', [['commerce-example','Nạp ví dụ còn trả 220.000đ']]],
    ['Combo và ưu đãi',[['commerce-empty-cart','Giỏ combo rỗng'],['commerce-stock','Bật/tắt hết hàng nước riêng'],['commerce-promo-success','Mã CINE20 hợp lệ'],['commerce-promo-invalid','Mã không hợp lệ'],['commerce-promo-ineligible','Mã không đủ điều kiện']]],
    ['Số dư hoàn vé',[['commerce-credit-zero','Số dư 0đ và lịch sử trống'],['commerce-credit-partial','Credit một phần: dùng 100.000đ'],['commerce-credit-full','Credit đủ toàn bộ: còn trả 0đ']]],
    ['Kết quả thanh toán',[['commerce-success','Thành công'],['commerce-failed','Thất bại'],['commerce-unknown','Chưa xác định kết quả'],['commerce-cancelled','Người dùng hủy']]],
    ['Đối soát và hết giờ',[['commerce-check','Kiểm tra lại → thành công'],['commerce-check-fail','Kiểm tra lại → thất bại'],['commerce-expire-before','Hết giờ trước thanh toán'],['commerce-expire-during','Hết giờ khi đang xử lý']]]
  ];
  group.innerHTML='<h2>Combo · Checkout · Credit</h2><p>Nạp ví dụ để có Couple J1–J2, combo 100.000đ, mã giảm 20.000đ và dùng Credit 100.000đ. Nút kết quả đặt trạng thái cho lần trả tiền tiếp theo, hoặc giải quyết giao dịch đang chờ. Nạp ví dụ tạo phiên mẫu mới.</p><div class="preview-state"><div><span>Còn thanh toán</span><strong id="commerce-preview-due">—</strong></div><div><span>Số dư khả dụng</span><strong id="commerce-preview-balance">150.000đ</strong></div><div><span>Credit áp dụng</span><strong id="commerce-preview-credit">0đ</strong></div><div><span>Giao dịch</span><strong id="commerce-preview-status">Chưa bắt đầu</strong></div><p id="commerce-preview-order">Chưa có giao dịch</p></div>'+sections.map(([title,items])=>'<section class="commerce-preview-group"><h3>'+title+'</h3>'+items.map(([action,label])=>'<button type="button" data-commerce-scenario="'+action+'">'+label+'</button>').join('')+'</section>').join('');
  const intro=document.querySelector('.preview-intro');intro.after(group);
  group.querySelectorAll('[data-commerce-scenario]').forEach(button=>button.addEventListener('click',()=>{
    frame.contentWindow.postMessage({channel,type:'commerce-scenario',action:button.dataset.commerceScenario},location.origin==='null'?'*':location.origin);
    document.getElementById('preview-message').textContent='Đã gửi: '+button.textContent+'.';
  }));
  const labels={idle:'Chưa bắt đầu',processing:'Đang xử lý',success:'Thành công',failed:'Thất bại',unknown:'Đang kiểm tra',cancelled:'Đã hủy'};
  let lastMessage='';
  window.addEventListener('message',event=>{
    if(event.source!==frame.contentWindow||event.origin!==location.origin||event.data?.channel!==channel||event.data?.type!=='commerce-state')return;
    const s=event.data.payload,format=n=>new Intl.NumberFormat('vi-VN').format(n)+'đ';
    document.getElementById('commerce-preview-due').textContent=format(s.amountDue);
    document.getElementById('commerce-preview-balance').textContent=format(s.balance);
    document.getElementById('commerce-preview-credit').textContent=format(s.credit);
    document.getElementById('commerce-preview-status').textContent=labels[s.payment]||s.payment;
    document.getElementById('commerce-preview-order').textContent=s.order||'Chưa có giao dịch';
    if(s.message&&s.message!==lastMessage){lastMessage=s.message;document.getElementById('preview-message').textContent=s.message;}
  });
})();
