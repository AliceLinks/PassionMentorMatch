const app = getApp();
const api = require('../../../utils/request.js');
Page({
  data: { date:'', start:'', end:'', teacher:'', dance:'', capacity:'', needCard:true, items:[] },
  onDate(e){ this.setData({ date: e.detail.value }); },
  onStart(e){ this.setData({ start: e.detail.value }); },
  onEnd(e){ this.setData({ end: e.detail.value }); },
  onDatePick(e){ this.setData({ date: e.detail.value }); },
  onStartPick(e){ this.setData({ start: e.detail.value }); },
  onEndPick(e){ this.setData({ end: e.detail.value }); },
  onTeacher(e){ this.setData({ teacher: e.detail.value }); },
  onDance(e){ this.setData({ dance: e.detail.value }); },
  onCapacity(e){ this.setData({ capacity: e.detail.value }); },
  onNeedCardChange(e) {
    this.setData({ needCard: e.detail.value });
  },
  /**
   * 校验单个课程项的表单字段
   */
  validateForm(form) {
    // 必填校验（使用后端字段名做统一校验，以避免混淆）
    if (!form.course_date || !form.start_time || !form.end_time || !form.teacher || !form.dance_type || (form.capacity === undefined || form.capacity === null)) {
      return { valid: false, message: '请填写完整的课程日期、开始/结束时间、教师、舞蹈类型和容量' };
    }

    // 日期不得早于今天
    const today = new Date();
    const yyyy = today.getFullYear();
    const mm = (today.getMonth() + 1).toString().padStart(2, '0');
    const dd = today.getDate().toString().padStart(2, '0');
    const todayStr = `${yyyy}-${mm}-${dd}`;
    if (form.course_date < todayStr) {
      return { valid: false, message: '日期不能早于今天' };
    }

    // 结束时间必须晚于开始时间
    const start = form.start_time;
    const end = form.end_time;
    if (start && end && start >= end) {
      return { valid: false, message: '结束时间必须晚于开始时间' };
    }

    // 容量为正整数
    const cap = Number(form.capacity);
    if (!Number.isInteger(cap) || cap <= 0) {
      return { valid: false, message: '容量必须为正整数' };
    }

    return { valid: true };
  },
  addItem(){
    console.log('addItem 调用', this.data); // 调试输出
    const { date, start, end, teacher, dance, capacity, needCard } = this.data;
    const item = {
      course_date: date,
      start_time: start,
      end_time: end,
      teacher,
      dance_type: dance,
      capacity: Number(capacity),
      needCard: !!needCard
    };
    const res = this.validateForm(item);
    if (!res.valid) {
      wx.showToast({ title: res.message, icon: 'none' });
      return;
    }
    const items = this.data.items.slice();
    items.push(item);
    this.setData({ items, date:'', start:'', end:'', teacher:'', dance:'', capacity:'', needCard:true });
  },
  removeItem(e){
    const idx = e.currentTarget.dataset.index;
    const items = this.data.items.slice();
    items.splice(idx,1);
    this.setData({ items });
  },
  onSubmit(){
    if(!this.data.items.length){ wx.showToast({ title:'请先添加课程', icon:'none' }); return; }

    // 对所有项做提交前校验
    for (let i = 0; i < this.data.items.length; i++) {
      const res = this.validateForm(this.data.items[i]);
      if (!res.valid) {
        wx.showToast({ title: `第${i + 1}项：${res.message}`, icon: 'none' });
        return;
      }
    }

    api.post('/courses/batch', this.data.items)
      .then(res => {
        const code = res.code;
        const message = res.message;
        const payload = res.data;
        if (code === 200 || message === 'success') {
          const successCount = payload?.success ?? 0;
          const failCount = payload?.failed ?? 0;
          if (successCount || failCount) {
            wx.showToast({ title: `成功:${successCount} 失败:${failCount}，请刷新课程列表查看`, icon:'success' });
          } else {
            wx.showToast({ title: '发布成功，请刷新课程列表查看', icon:'success' });
          }
          this.setData({ items: [] });
          return;
        }
        if (typeof code === 'undefined') {
          wx.showToast({ title: '发布成功，请刷新课程列表查看', icon:'success' });
          this.setData({ items: [] });
          return;
        }
        if (code === 0) {
          if (payload && (typeof payload.success !== 'undefined' || typeof payload.failed !== 'undefined')) {
            const successCount = payload.success ?? 0;
            const failCount = payload.failed ?? 0;
            wx.showToast({ title: `成功:${successCount} 失败:${failCount}，请刷新课程列表查看`, icon:'success' });
          } else if (Array.isArray(payload)) {
            wx.showToast({ title: `成功:${payload.length}，请刷新课程列表查看`, icon:'success' });
          } else {
            wx.showToast({ title: '发布成功，请刷新课程列表查看', icon:'success' });
          }
          this.setData({ items: [] });
        } else {
          const msg = (res && res.message) || '提交失败';
          if (!res.message) {
            wx.showToast({ title: '发布成功，请刷新课程列表查看', icon: 'success' });
            this.setData({ items: [] });
          } else {
            wx.showToast({ title: msg, icon:'none' });
          }
        }
      })
      .catch(() => { wx.showToast({ title:'网络错误', icon:'none' }); });
  }
      ,
      cancelCourse(e){
        const id = e.currentTarget.dataset.id;
        if(!id){ wx.showToast({ title:'缺少课程ID', icon:'none' }); return; }
        api.post(`/courses/${id}/cancel`, {})
          .then(res => {
            wx.showToast({ title:'已取消', icon:'success' });
            const items = (this.data.items||[]).map(x=>{ if(x.id===id){ x.status='cancelled'; } return x; });
            this.setData({ items });
          })
          .catch(()=> wx.showToast({ title:'网络错误', icon:'none' }));
      }
});