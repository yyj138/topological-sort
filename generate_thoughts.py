from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH

doc = Document()

# 标题
title = doc.add_heading('个人感想（易雨杰）', 0)
title.alignment = WD_ALIGN_PARAGRAPH.CENTER

doc.add_paragraph()

doc.add_heading('一、个人工作内容', level=1)
doc.add_paragraph('作为小组的B模块负责人，同时也是项目的统筹协调人，我在这次项目中主要完成了以下工作：')

p = doc.add_paragraph(style='List Number')
p.add_run('项目前期：').bold = True
p.add_run('从最开始的任务拆分、5人分工表制定，到GitHub仓库搭建、分支规范制定、接口契约文档起草，再到README和项目文档的编写，这些基础性工作都是我牵头完成的。从一开始就明确了每个人的任务边界和交付物，避免了后面扯皮。')

p = doc.add_paragraph(style='List Number')
p.add_run('GUI模块开发：').bold = True
p.add_run('负责整个GUI主框架的设计和实现，包括MainFrame主窗口布局、InputPanel输入面板、ResultPanel结果面板（分页+5色轮换联动）、StatusBar状态栏（节点/边数/停止原因/缩放比例显示）、MainController主控制器（任务调度、线程管理、模块间数据传递）。这部分是我花时间最多的，从界面布局到交互逻辑，前后改了十多版。')

p = doc.add_paragraph(style='List Number')
p.add_run('代码合并与集成：').bold = True
p.add_run('每个人在自己的分支开发，我负责把A的算法、C的可视化、D的IO模块合并到dev-b分支，解决合并冲突，保证所有模块能正常对接。前后合并了十多次，每次有新的提交我就拉下来测试。')

p = doc.add_paragraph(style='List Number')
p.add_run('测试与反馈：').bold = True
p.add_run('负责B/C/D衔接场景的测试，写了《B交互与任务衔接场景检查反馈》，把6个测试点都测了一遍，发现问题及时反馈给对应的同学修改。比如状态栏缺少停止原因提示、图例颜色不跟随、边没有高亮这些问题，都是我在测试中发现的。')

p = doc.add_paragraph(style='List Number')
p.add_run('会议组织：').bold = True
p.add_run('牵头组织了三次小组会议，每次会议前整理讨论议题，会后写会议记录，跟进每个人的任务进度，确保项目按计划推进。')

doc.add_heading('二、技术上的成长与收获', level=1)

doc.add_heading('1. 从理论到实践的算法理解', level=2)
doc.add_paragraph('之前在数据结构课上学过Kahn拓扑排序算法，只是知道个大概，这次真正自己做项目，才发现原来要考虑的东西这么多：')
p = doc.add_paragraph(style='List Bullet')
p.add_run('为什么要枚举所有拓扑排序？').bold = True
p.add_run('一开始我以为拓扑排序只有一个结果，后来才知道当节点之间没有依赖关系时，排列组合会有很多种。比如figure1有15个节点，就有3万多种合法的拓扑排序，这个数量级的差距让我对算法的应用场景有了新的认识。')

p = doc.add_paragraph(style='List Bullet')
p.add_run('回溯算法的实际应用：').bold = True
p.add_run('枚举所有拓扑排序的核心是回溯算法，每次选一个入度为0的节点，然后递归继续，选完了就回溯。这个过程中还要考虑上限、超时、取消这些实际工程问题，不是教科书上写的那么简单。')

doc.add_heading('2. Java Swing的实战经验', level=2)
doc.add_paragraph('之前我对Swing的了解只停留在课本上的小例子，这次真正做一个完整的桌面应用，踩了很多坑：')
p = doc.add_paragraph(style='List Bullet')
p.add_run('事件 dispatch 线程：').bold = True
p.add_run('一开始计算直接在界面线程里跑，图一复杂整个界面就卡死了，点什么都没反应。后来才知道要把耗时操作放到SwingWorker后台线程，计算完再在EDT里更新界面，这个坑踩了好久才明白。')

p = doc.add_paragraph(style='List Bullet')
p.add_run('布局管理器：').bold = True
p.add_run('Swing的布局管理器一开始用得很别扭，BorderLayout、BoxLayout、FlowLayout混着用，界面总是歪歪扭扭的。后来一点点调，才把主窗口的工具栏、输入区、图区、结果区、状态栏摆得比较合理。')

p = doc.add_paragraph(style='List Bullet')
p.add_run('自绘组件：').bold = True
p.add_run('GraphPanel是自绘的，节点怎么画、边怎么画、箭头怎么画、高亮怎么画，都是一点点调出来的。特别是字体、颜色、圆角这些细节，改了无数版才看着舒服。')

doc.add_heading('3. 软件工程思维的建立', level=2)
p = doc.add_paragraph(style='List Bullet')
p.add_run('接口契约的重要性：').bold = True
p.add_run('一开始大家各写各的，A的算法输出和B的GUI输入对不上，C的可视化接口和B的调用对不上，浪费了很多时间。后来A牵头写了接口契约文档，把每个模块的输入输出、方法签名都定下来，大家按契约写，后面就顺了很多。这让我深刻体会到，前期设计比写代码更重要。')

p = doc.add_paragraph(style='List Bullet')
p.add_run('版本控制的实践：').bold = True
p.add_run('之前用Git只是简单的add/commit/push，这次多人协作才真正体会到分支管理的重要性。每个人一个分支，定期合并到集成分支，解决冲突，Code Review，这套流程走下来，对Git的理解深了很多。')

p = doc.add_paragraph(style='List Bullet')
p.add_run('模块化设计：').bold = True
p.add_run('把项目分成算法、GUI、可视化、IO、测试五个模块，每个模块只暴露接口，内部怎么实现别人不用管。这样一个人改代码不会影响其他人，调试的时候也能定位到具体模块，不会乱成一锅粥。')

doc.add_heading('三、团队合作的体会', level=1)
doc.add_paragraph('这次五人小组合作，和我之前做的课设完全不一样，最大的感受是：团队合作不是简单的把任务切完各做各的就完事了，中间需要大量的沟通和对齐。')

p = doc.add_paragraph(style='List Bullet')
p.add_run('沟通比写代码重要：').bold = True
p.add_run('很多时候不是技术问题，是理解不一致。比如B以为C的接口是单参数，C以为B要传两个参数，这种问题如果不及时对齐，写出来的代码根本接不上。我们后来约定，接口有改动必须第一时间在群里说，不能自己悄悄改。')

p = doc.add_paragraph(style='List Bullet')
p.add_run('文档是团队的记忆：').bold = True
p.add_run('如果没有接口契约文档、任务分工文档、会议记录，过两周大家就忘了之前讨论了什么、定了什么。每次开会把结论写下来，谁负责做什么、什么时候交，写在文档里，后面就有据可查，不用每次都翻聊天记录。')

p = doc.add_paragraph(style='List Bullet')
p.add_run('互相补位：').bold = True
p.add_run('这次项目里，我除了做自己的GUI模块，还帮大家协调进度、合并代码、测试衔接。其他同学也是，比如C做可视化的时候，发现接口不对，会主动和我商量怎么改；D写导出的时候，会主动问B这边传什么参数比较好。这种主动沟通的氛围特别好。')

p = doc.add_paragraph(style='List Bullet')
p.add_run('接受反馈：').bold = True
p.add_run('自己写的代码，自己看着挺好，但别人一用就发现各种问题。比如我一开始写的状态栏，没显示停止原因，D测试的时候发现了；图例太大占地方，我自己用着没感觉，别人一看就觉得不对。接受别人的反馈，改完之后确实更好用了。')

doc.add_heading('四、不足与未来改进', level=1)
p = doc.add_paragraph(style='List Number')
p.add_run('前期沟通还是不够充分：').bold = True
p.add_run('一开始接口定得比较粗，后面改了好几次，浪费了一些时间。下次做项目，前期一定要把接口和数据格式定死，不能边写边改。')

p = doc.add_paragraph(style='List Number')
p.add_run('测试不够自动化：').bold = True
p.add_run('大部分测试都是手动点的，没有写单元测试。每次改一点代码就要手动测一遍，很费时间。下次应该一开始就把单元测试写好，改代码跑一遍测试就知道有没有问题。')

p = doc.add_paragraph(style='List Number')
p.add_run('时间管理可以更好：').bold = True
p.add_run('前期进度比较慢，中期才赶上来。下次应该更早把进度排出来，每周check一下，不要临到期了才赶。')

doc.add_heading('五、总结', level=1)
doc.add_paragraph('这次高级算法项目，从最开始的一行代码都没有，到现在做出一个能完整跑起来的拓扑排序工具，整个过程非常充实。不仅把课本上学的算法真正落地了，更重要的是学会了怎么和团队一起做一个完整的软件项目——从需求到设计到编码到测试到交付，每个环节都走了一遍。')

doc.add_paragraph('特别感谢小组的其他四位同学：A负责核心算法和架构，把最硬的骨头啃下来了；C负责可视化，把图画得越来越好看；D负责IO和解析，各种边界情况都考虑到了；E负责测试和文档，帮我们查漏补缺。没有大家的配合，这个项目不可能做完。')

doc.add_paragraph('这次项目的经历，不管是技术上还是团队合作上，都让我成长了很多，对以后做更大的项目也更有信心了。')

doc.save('E:\\tp\\docs\\个人感想-易雨杰.docx')
print('done')
