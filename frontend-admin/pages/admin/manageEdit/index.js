const api = require('../../../utils/request.js');
Page({
  data: {
    id: '',
    title: '',
    time: '',
    loading: false
  },
  onLoad(query) {
    if(query && query.id){
      this.setData({ id: query.id });
      this.loadDetail(query.id);
    }
  },
  weekStart() {
    const date = new Date();
    date.setDate(date.getDate() - date.getDay() + 1); // 计算本周一的日期
    return date.toISOString().split('T')[0]; // 格式化为YYYY-MM-DD
  },
  loadDetail(id){
    console.log('loadDetail 调用，id:', id); // 调试输出
    api.get('/courses/week', { week_start: this.weekStart() })
      .then(res => {
        const list = res.courses || [];
        console.log('课程列表:', list, '查找id:', id); // 调试输出
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
          });
        } else {
          wx.showToast({ title:'未找到课程', icon:'none' });
        }
      })
      .catch(err => {
        console.log('请求失败', err); // 调试输出
        wx.showToast({ title:'获取课程失败', icon:'none' })
      });
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
  onTitleInput(e){ this.setData({ title: e.detail.value }); },
  onTimeInput(e){ this.setData({ time: e.detail.value }); },
  onSave() {
    console.log('onSave', this.data); // 调试输出
    // 检查 title 和 time 是否为空或全是空格
    if(!this.data.teacher || !this.data.dance_type || !this.data.capacity || !this.data.course_date || !this.data.start_time || !this.data.end_time){
      wx.showToast({ title:'请填写完整', icon:'none' });
      return;
    }
    this.setData({ loading: true });
    api.put('/courses/' + this.data.id, {
      teacher: this.data.teacher,
      dance_type: this.data.dance_type,
      capacity: Number(this.data.capacity),
      course_date: this.data.course_date,
      start_time: this.data.start_time,
      end_time: this.data.end_time
    }).then(res => {
      wx.showToast({ title: (res.message || '保存成功'), icon:'none' });
      wx.navigateBack({ delta: 1 });
    }).finally(() => {
      this.setData({ loading: false });
    });
  }
});