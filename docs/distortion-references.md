# AI 识别认知歪曲的参考资料

核对日期：2026-10-14。以下来源均为 David D. Burns 本人官网发布的清单或文章正文；未将访客评论、第三方总结或模型自行补写的标准当作作者说明。

## 运行时内容

识别请求的 `认知歪曲参考说明` 来自 `CbtReferences.distortionGuide()`，各类型包含核心含义、补充说明和来源链接。内容依据英文资料独立摘要为中文，并非官方中文译文，也不是整表复制。链接提供核对依据，摘要本身已经随请求发送，模型无需打开网页才能读取说明。

详细资料只在“识别认知歪曲”请求发送，界面保留简短介绍。AI 按当前想法和情境列出有明确依据的适用类型，不限定 3 种，不凑数；理由说明当前判断为何符合类型，而不是复述定义。返回结果仍校验名称、理由、去重和结构。方法推荐继续最多 3 种。

## 逐项来源

| 应用类型 | 官方资料 | 本轮核对的重点 |
| --- | --- | --- |
| 非黑即白 | [第 189 期](https://feelinggood.com/2020/05/04/189-how-to-crush-negative-thoughts-all-or-nothing-thinking/) | 极端类别与中间程度。 |
| 以偏概全 | [第 190 期](https://feelinggood.com/2020/05/11/190-how-to-crush-negative-thoughts-overgeneralization/)、[Secrets of Self-Esteem #2](https://feelinggood.com/2014/01/06/secrets-of-self-esteem-2-negative-and-positive-distortions/) | 从具体事件扩展到整体自我或未来；与标签的关系。 |
| 心理过滤、否定积极 | [第 191 期](https://feelinggood.com/2020/05/18/191-how-to-crush-negative-thoughts-mental-filter-discounting-the-positive/) | 忽略与否定积极信息的区别。 |
| 读心术、预言未来 | [第 194 期](https://feelinggood.com/2020/06/08/194-jumping-to-conclusions/) | 缺少证据的跳跃结论，分别涉及他人想法与未来结果。 |
| 夸大或缩小 | [第 195 期](https://feelinggood.com/2020/06/15/195-how-to-crush-negative-thoughts-magnification-and-minimization/) | 对程度或分量的放大与缩小。 |
| 情绪化推理 | [第 203 期](https://feelinggood.com/2020/08/10/203-how-to-crush-negative-thoughts-emotional-reasoning/) | 从感受推断事实；作者同时肯定表达感受的价值。 |
| 应该思维 | [第 205 期](https://feelinggood.com/2020/08/24/205-how-to-crush-negative-thoughts-should-statements/) | 明示与隐含的要求，以及道德、法律、自然规律三类有效用法。 |
| 贴标签 | [第 199 期](https://feelinggood.com/2020/07/25/199-how-to-crush-negative-thoughts-labeling-2/) | 对自己或他人的整体标签、角色标签与具体行为描述。 |
| 自责或责怪他人 | [作者清单](https://feelinggood.com/wp-content/uploads/2017/04/41656-distortions-v-18.pdf)、[第 206 期](https://feelinggood.com/2020/08/31/206-how-to-crush-negative-thoughts-blame/) | 两类归责及各自常伴随的应该思维。 |

Burns 的基础清单按十类组织。应用将“跳跃结论”的读心术、预言未来分别列出，将自责、责怪他人合为一个选项，因此有 11 个候选名称；这是应用的呈现方式。

## 边界与取舍

[第 373 期](https://feelinggood.com/2023/12/04/373-why-therapy-fails/)在初始评估部分讨论健康的负面情绪，在方法错误第 12 项提醒负面想法可能符合事实。该提醒连同来源一起发送，避免预设用户一定存在歪曲。

资料未给每种歪曲都列一份完整的排除标准，本轮不补造这种表格。运行时补充只概括上述来源明确讨论的区别、形式和例外。对照定义、要求具体理由，以及不按关键词或已有勾选机械判断，是本应用的任务约束，并非宣称作者提供了这套固定的 AI 分类规则。

## 验证范围

`bash tools/test.sh` 检查超过 3 种结果的解析与写入、重复类型处理、全部候选的说明和来源随识别请求发送，以及其他请求不附带整份识别资料。测试不等于对模型实际判断质量或临床效果的验证。完整运行时内容随 [ai-prompts-1.27.md](ai-prompts-1.27.md) 导出。
