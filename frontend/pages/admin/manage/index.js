Page({
  data:{ courses:[] },
  onShow(){ this.fetchCourses(); },
  fetchCourses(){
    const base = wx.getStorageSync('BASE_URL');
    if(!base){ wx.showToast({ title:'未配置BASE_URL', icon:'none' }); return; }
    const apiPrefix = base.endsWith('/api') ? base : base + '/api';
    wx.request({
      url: apiPrefix + '/courses/week?week_start=' + this.weekStart(),
      method: 'GET',
      header: { 'Authorization': `Bearer ${wx.getStorageSync('ADMIN_TOKEN')}` },
      success: (res)=>{
        const list = res?.data?.data?.courses || [];
        const published = list.filter(x=>x.status==='published');
        this.setData({ courses: published });
      }
    });
  },
  weekStart(){
    const d = new Date();
    const day = d.getDay() || 7; // 周一为一周开始
    const start = new Date(d);
    start.setDate(d.getDate() - (day-1));
    const y = start.getFullYear();
    const m = String(start.getMonth()+1).padStart(2,'0');
    const dd = String(start.getDate()).padStart(2,'0');
    return `${y}-${m}-${dd}`;
  },
  onCancel(e){
    const id = e.currentTarget.dataset.id;
    const base = wx.getStorageSync('BASE_URL');
    const apiPrefix = base.endsWith('/api') ? base : base + '/api';
    wx.showModal({ title:'确认取消', content:'将取消课程并批量取消预约，继续？', success:(r)=>{
      if(!r.confirm) return;
      wx.request({
        url: `${apiPrefix}/courses/${id}/cancel`,
        method: 'POST',
        header: { 'Authorization': `Bearer ${wx.getStorageSync('ADMIN_TOKEN')}` },
        success: (res)=>{
          if(res.statusCode===200){
            wx.showToast({ title:'已取消', icon:'success' });
            this.fetchCourses();
          } else {
            wx.showToast({ title: res.data?.message || '取消失败', icon:'none' });
          }
        },
        fail: ()=> wx.showToast({ title:'网络错误', icon:'none' })
      });
    }});
  },
  onEdit(e){
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/admin/manageEdit/index?id=${id}` });
  }
  ,
  onRoster(e){
    const id = e.currentTarget.dataset.id;
    if(!id){ wx.showToast({ title:'缺少课程ID', icon:'none' }); return; }
    wx.navigateTo({ url: `/pages/admin/roster/index?courseId=${id}` });
  }
});