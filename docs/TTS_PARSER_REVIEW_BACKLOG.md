# TTS parser review backlog

Отложенная ручная ревизия файлов проекта TTS, на которых после исправлений
парсера от 30 июля 2026 года всё ещё остаётся хотя бы одна синтаксическая
диагностика.

- Корень путей: `/Users/dvamuch/work-projects/TTS`
- Контрольный коммит плагина: `abd6a1d`
- Всего проверено: 2477 `.asp` / `.inc` файлов
- Без диагностик: 2425 файлов
- Требуют повторной проверки: 52 файла

Текущая гипотеза: значительная часть списка содержит реальные ошибки исходного
кода или не-VBScript данные, но нельзя считать это доказанным для каждого файла.
При возвращении к задаче нужно вручную разделить список на три группы:

1. подтверждённая ошибка в исходном файле TTS;
2. валидный VBScript, который ещё не поддержан плагином;
3. служебный, архивный или не-VBScript файл, который следует исключить из аудита.

## Файлы

```text
500-100.asp
admin/linksu2o.asp
Cables/index.asp
Cables/index1.asp
Cables/index_sl.asp
Connect-1.asp
customers/customerbillingconf.asp
customers/customerBizservices.asp
customers/customerinfo.asp
customers/customerservices.asp
customers/customerservices_old.asp
customers/ordermsg2billing.asp
customers/orderrespinfo.asp
customers/ordersRepor-type12-xls.asp
devices/configdelete.asp
devices/Tula_devices.asp
files/update.asp
inc/ yfunctions.asp
inc/500-101.asp
inc/functions.asp
inc/functions_sl.asp
inc/Phone_sl.asp
inc/yfunctions.asp
monitoring/GroupList.asp
network/aspointadd_.asp
network/networkroutesr.asp
PhoneNumbers/functions.asp
services/sendmail_pservice_memo.asp
services/serviceitemsListchangestatus1.asp
smt/lang/lang.it.pack/_vti_cnf/smt.base.asp
smt/lang/lang.pt.asp
spam/filters.asp
test/SLTestxls.asp
test/test16.asp
test/test17.asp
test/Test_Function.asp
troubles/ContactAdd2Trouble.asp
troubles/ContactAdd2Trouble_old.asp
troubles/troubleadd_b.asp
troubles/troubleadd_old.asp
troubles/TroubleChanges_steps1-3.asp
troubles/troublechangestatus.asp
troubles/troublechangestatus1.asp
troubles/troublechangestatus_b.asp
troubles/troublechangestatus_sl.asp
troubles/troublereport.asp
troubles/troublereport_b.asp
troubles/troublereport_sl.asp
troubles/troublereportxls_new.asp
users/sms_new1.asp
users/user2groupadd.asp
users/userorders_sl3.asp
```

## Условия возврата к задаче

- не расширять грамматику только ради подавления диагностики;
- сначала подтвердить конструкцию на реальном движке VBScript или по документации;
- для каждого исправленного пробела добавлять минимальный regression-тест;
- после каждого пакета повторять полный аудит TTS и проверку файлов, которые
  раньше приводили к медленному разбору.
