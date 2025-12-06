Page({
  data:{ id:null, course_date:'', start_time:'', end_time:'', teacher:'', dance_type:'', capacity:'', pickerMinDate:'' },
  onLoad(q){ this.setData({ id: q.id }); this.loadDetail(q.id); },
  loadDetail(id){
    // 简化：复用周列表并筛选单项
    const base = wx.getStorageSync('BASE_URL');
    const apiPrefix = base.endsWith('/api') ? base : base + '/api';
    wx.request({
      url: apiPrefix + '/courses/week?week_start=' + this.weekStart(),
      method:'GET', header:{ 'Authorization': `Bearer ${wx.getStorageSync('ADMIN_TOKEN')}` },
      success:(res)=>{
        const list = res?.data?.data?.courses || [];
        const item = list.find(x=>String(x.id)===String(id));
        if(item){
          const today = new Date();
          const yyyy = today.getFullYear();
          const mm = String(today.getMonth()+1).padStart(2,'0');
          const dd = String(today.getDate()).padStart(2,'0');
          const minDate = `${yyyy}-${mm}-${dd}`;
          this.setData({
          course_date:item.course_date||'', start_time:item.start_time||'', end_time:item.end_time||'',
          teacher:item.teacher||'', dance_type:item.dance_type||'', capacity:item.capacity||'', pickerMinDate:minDate
        }); }
      }
    });
  },
  weekStart(){
    const d = new Date(); const day = d.getDay()||7; const start = new Date(d); start.setDate(d.getDate()-(day-1));
    const y=start.getFullYear(), m=String(start.getMonth()+1).padStart(2,'0'), dd=String(start.getDate()).padStart(2,'0'); return `${y}-${m}-${dd}`;
  },
  onDate(e){ this.setData({ course_date: e.detail.value }); },
  onDatePick(e){ this.setData({ course_date: e.detail.value }); },
  onStart(e){ this.setData({ start_time: e.detail.value }); },
  onStartPick(e){ this.setData({ start_time: e.detail.value }); },
  onEnd(e){ this.setData({ end_time: e.detail.value }); },
  onEndPick(e){ this.setData({ end_time: e.detail.value }); },
  onTeacher(e){ this.setData({ teacher: e.detail.value }); },
  onDance(e){ this.setData({ dance_type: e.detail.value }); },
  onCapacity(e){ this.setData({ capacity: e.detail.value }); },
  onSave(){
    const base = wx.getStorageSync('BASE_URL');
    const apiPrefix = base.endsWith('/api') ? base : base + '/api';
    const payload = {
      course_date:this.data.course_date,
      start_time:this.data.start_time,
      end_time:this.data.end_time,
      teacher:this.data.teacher,
      dance_type:this.data.dance_type,
      capacity:Number(this.data.capacity)
    };
    wx.request({
      url: `${apiPrefix}/courses/${this.data.id}`,
      method: 'PUT',
      data: payload,
      header: { 'content-type':'application/json', 'Authorization': `Bearer ${wx.getStorageSync('ADMIN_TOKEN')}` },
      success:(res)=>{
        if(res.statusCode===200){ wx.showToast({ title:'已保存', icon:'success' }); wx.navigateBack(); }
        else { wx.showToast({ title: res.data?.message||'保存失败', icon:'none' }); }
      },
      fail:()=> wx.showToast({ title:'网络错误', icon:'none' })
    });
  }
});