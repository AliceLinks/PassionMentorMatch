Page({
  data:{ courseId:'', list:[], courses:[], selectedIndex:0, weekStart:'' },
  onShow(){
    const token = wx.getStorageSync('ADMIN_TOKEN');
    if(!token){
      wx.showToast({ title:'请先登录管理员账号', icon:'none' });
      wx.redirectTo({ url: '/pages/admin/login/index' });
    }
    // 默认以今天所在周为 week_start
    const today = new Date();
    const yyyy = today.getFullYear();
    const mm = (today.getMonth()+1).toString().padStart(2,'0');
    const dd = today.getDate().toString().padStart(2,'0');
    const weekStart = getWeekStart(`${yyyy}-${mm}-${dd}`);
    this.setData({ weekStart });
    this.fetchWeekCourses(weekStart);
  },
  onCourseId(e){ this.setData({ courseId: e.detail.value }); },
  onPickerChange(e){
    const idx = Number(e.detail.value || 0);
    const course = this.data.courses[idx];
    this.setData({ selectedIndex: idx, courseId: course?.id || '' });
  },
  fetchWeekCourses(weekStart){
    const base = wx.getStorageSync('BASE_URL');
    const token = wx.getStorageSync('ADMIN_TOKEN');
    const apiPrefix = base.endsWith('/api') ? base : `${base}/api`;
    wx.request({
      url: `${apiPrefix}/courses/week?week_start=${weekStart}`,
      method: 'GET',
      header: { 'Authorization': `Bearer ${token}` },
      success: (res) => {
        if(res.statusCode===200 && res.data && (res.data.code===0 || res.data.code===200)){
          const arr = (res.data.data?.courses || []).map(c => ({
            id: c.id,
            label: `${c.course_date} ${c.start_time?.slice(0,5) || ''} ${c.teacher || ''}（${c.dance_type || ''}）`
          }));
          this.setData({ courses: arr });
          if(arr.length){ this.setData({ courseId: arr[0].id, selectedIndex: 0 }); }
        } else { wx.showToast({ title: (res.data && res.data.message) || '获取课程失败', icon:'none' }); }
      },
      fail: () => wx.showToast({ title:'网络错误', icon:'none' })
    });
  },
  async onQuery(){
    const id = this.data.courseId;
    if(!id){ wx.showToast({ title:'请先选择课程', icon:'none' }); return; }
    try{
      const base = wx.getStorageSync('BASE_URL');
      const apiPrefix = base.endsWith('/api') ? base : `${base}/api`;
      wx.request({
        url: `${apiPrefix}/reservations/course/${id}`,
        method: 'GET',
        header: { 'Authorization': `Bearer ${wx.getStorageSync('ADMIN_TOKEN')}` },
        success: (res) => {
          if(res.statusCode===200 && res.data && (res.data.code===0 || res.data.code===200)){
            this.setData({ list: res.data.data?.list || [] });
          } else { wx.showToast({ title: (res.data && res.data.message) || '查询失败', icon:'none' }); }
        },
        fail: () => wx.showToast({ title:'网络错误', icon:'none' })
      });
    } catch(err){ wx.showToast({ title:'网络错误', icon:'none' }); }
  }
});

function getWeekStart(dateStr){
  const d = new Date(dateStr);
  const day = d.getDay();
  const diff = (day === 0 ? -6 : 1) - day; // 周一为一周起始
  d.setDate(d.getDate() + diff);
  const yyyy = d.getFullYear();
  const mm = (d.getMonth()+1).toString().padStart(2,'0');
  const dd = d.getDate().toString().padStart(2,'0');
  return `${yyyy}-${mm}-${dd}`;
}