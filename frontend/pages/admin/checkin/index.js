Page({
  data:{ rid:'', scanResult:'' },
  onShow(){
    const token = wx.getStorageSync('ADMIN_TOKEN');
    if(!token){
      wx.showToast({ title:'请先登录管理员账号', icon:'none' });
      wx.redirectTo({ url: '/pages/admin/login/index' });
    }
  },
  onRid(e){ this.setData({ rid: e.detail.value }); },
  onScan(){
    const token = wx.getStorageSync('ADMIN_TOKEN');
    if(!token){ wx.showToast({ title:'请先登录管理员', icon:'none' }); return; }
    wx.scanCode({
      onlyFromCamera: true,
      scanType: ['qrCode','barCode'],
      success: (res) => {
        const text = res.result || '';
        this.setData({ scanResult: text });
        try{
          const obj = JSON.parse(text);
          if(obj.reservation_id){ this.setData({ rid: obj.reservation_id }); }
          if(obj.code){ this.checkinWithCode(obj.code); return; }
          if(obj.reservation_id){ this.onCheckin(); }
        } catch(e){
          if(/^[0-9]+$/.test(text)){ this.setData({ rid: text }); this.onCheckin(); }
          else { wx.showToast({ title:'二维码格式不支持', icon:'none' }); }
        }
      },
      fail: () => wx.showToast({ title:'扫码失败', icon:'none' })
    });
  },
  checkinWithCode(code){
    const base = wx.getStorageSync('BASE_URL');
    const apiPrefix = base.endsWith('/api') ? base : `${base}/api`;
    wx.request({
      url: `${apiPrefix}/checkin`,
      method: 'POST',
      data: { code },
      header: {
        'content-type': 'application/json',
        'Authorization': `Bearer ${wx.getStorageSync('ADMIN_TOKEN')}`
      },
      success: (res) => {
        if(res.statusCode===200 && res.data && (res.data.code===0 || res.data.code===200)){
          wx.showToast({ title:'签到成功' });
        } else { wx.showToast({ title: (res.data && res.data.message) || '签到失败', icon:'none' }); }
      },
      fail: () => wx.showToast({ title:'网络错误', icon:'none' })
    });
  },
  async onCheckin(){
    const rid = this.data.rid;
    if(!rid){ wx.showToast({ title:'请输入预约ID', icon:'none' }); return; }
    try{
      const base = wx.getStorageSync('BASE_URL');
      const apiPrefix = base.endsWith('/api') ? base : `${base}/api`;
      wx.request({
        url: `${apiPrefix}/checkin`,
        method: 'POST',
        data: { reservation_id: rid },
        header: {
          'content-type': 'application/json',
          'Authorization': `Bearer ${wx.getStorageSync('ADMIN_TOKEN')}`
        },
        success: (res) => {
          if(res.statusCode===200 && res.data && (res.data.code===0 || res.data.code===200)){
            wx.showToast({ title:'签到成功' });
          } else { wx.showToast({ title: (res.data && res.data.message) || '签到失败', icon:'none' }); }
        },
        fail: () => wx.showToast({ title:'网络错误', icon:'none' })
      });
    } catch(err){ wx.showToast({ title:'网络错误', icon:'none' }); }
  }
});