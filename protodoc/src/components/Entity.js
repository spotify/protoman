// @flow
import {Card, Icon, Spin} from "antd";
import React from 'react';
import ReactMarkdown from 'react-markdown';
import {connect} from 'react-redux';
import {Link} from "react-router-dom";
import type {Action} from 'redux';
import type {Entity as ModelEntity} from "../model";
import type {State} from '../reducers';
import './Entity.css';

type Props = {
  value: ModelEntity | null,
}

type CommentProps = { info?: { leadingComments: string }, short?: boolean };

type CommentState = { collapsed: boolean };

class Comment extends React.Component<CommentProps, CommentState> {
  state: CommentState = {
    collapsed: false
  };

  render() {
    const {info, short} = this.props;
    const {collapsed} = this.state;
    if (info && info.leadingComments) {
      if (short) {
        const source = info.leadingComments.split(/(\.|\n\n)/).filter(l => l.trim() !== '')[0];
        if (source) {
          return <ReactMarkdown className={'docblock-short'} source={source} />;
        } else {
          return null;
        }
      } else {
        const source = info.leadingComments;

        if (source.trim() !== '') {
          return [
            <a key="collapser" onClick={() => this.setState({collapsed: !collapsed})}
               style={{position: 'absolute', left: 0, color: 'rgba(0, 0, 0, 0.43)'}}>
              <Icon type={collapsed ? "plus-square-o" : "minus-square-o"} />
            </a>,
            collapsed
              ? <div key="collapsed-comment" className={'docblock'}><p>&hellip;</p></div>
              : <ReactMarkdown key="expanded-comment" className={'docblock'} source={source} />
          ];
        } else {
          return null;
        }
      }
    } else {
      return null;
    }
  }
}

const NestedEntityTable = ({title, entities}: { title: string, entities: ModelEntity[] }) => {
  return (
    <div>
      <h2>{title}</h2>
      <table>
        <tbody>
          {entities.map(child => (
            <tr key={child.fullName} className="module-item">
              <td>
                <Link to={formatUrl(child.fullName)}>
                  {child.descriptor.name}
                </Link>
              </td>
              <td>
                <Comment info={child.descriptor.$sourceCodeInfo} short={true} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

const formatUrl = fullName => {
  if (fullName === '') {
    return '/(root)';
  } else {
    return '/' + fullName.substr(1);
  }
};

const formatLinkedType = typeName => {
  const parts = typeName.split('.');
  return <Link to={formatUrl(typeName)}>{parts[parts.length - 1]}</Link>;
};

const formatFieldType = field => {
  let prefix = field.label.substr('LABEL_'.length).toLowerCase();
  if (prefix === 'optional') {
    prefix = '';
  } else {
    prefix += ' ';
  }

  let suffix;
  switch (field.type) {
    case 'TYPE_MESSAGE':
    case 'TYPE_GROUP':
    case 'TYPE_ENUM':
      suffix = formatLinkedType(field.typeName);
      break;
    default:
      suffix = field.type.substr('TYPE_'.length).toLowerCase();
  }

  return <span>{prefix}{suffix}</span>;
};

const formatLineInfo = (info?: { start: { line: number, column: number } }) => {
  if (info) {
    return `(${info.start.line}:${info.start.column})`;
  } else {
    return '';
  }
};

const streamingness = yep => {
  if (yep) {
    return 'streaming ';
  } else {
    return '';
  }
};

const Entity = (props: Props): React$Node => {
  const value: ModelEntity | null = props.value;
  if (value) {
    const descriptor = value.descriptor;

    const nestedPackages = value.children.filter(c => c.type === 'package');
    const nestedMessages = value.children.filter(c => c.type === 'message');
    const nestedEnums = value.children.filter(c => c.type === 'enum');
    const nestedServices = value.children.filter(c => c.type === 'service');
    const nestedOptions = value.children.filter(c => c.type === 'option');

    return <Card>
      <main className="component-entity-documentation">
        <h1>{value.type}&nbsp;<code>{descriptor.name}</code></h1>
        {value.sourceFiles.length > 0 && <p>
          <small>Defined
            in: {value.sourceFiles.join(', ')} {formatLineInfo(descriptor.$sourceCodeInfo)}</small>
        </p>}
        {value.type === 'message' && <pre>
          <span>message {value.descriptor.name} &#123;<br /></span>
          {value.descriptor.field.map(
            f => <span key={f.name}>&nbsp;&nbsp;{formatFieldType(f)}&nbsp;{
              f.name
            }&nbsp;=&nbsp;{f.number};<br /></span>)}
          &#125;
        </pre>}
        {value.type === 'enum' && <pre>
          <span>enum {value.descriptor.name} &#123;<br /></span>
          {value.descriptor.value.map(
            v => <span key={v.name}>&nbsp;&nbsp;{v.name}&nbsp;=&nbsp;{v.number};<br /></span>)}
          &#125;
        </pre>}
        {value.type === 'service' && <pre>
          <span>service {value.descriptor.name} &#123;<br /></span>
          {value.descriptor.method.map(
            m => <span key={m.name}>&nbsp;&nbsp;rpc&nbsp;{m.name}&nbsp;({
              streamingness(m.clientStreaming)
            }{
              formatLinkedType(m.inputType)
            })&nbsp;returns&nbsp;({
              streamingness(m.clientStreaming)
            }{
              formatLinkedType(m.outputType)
            });<br /></span>)}
          &#125;
        </pre>}
        <Comment info={descriptor.$sourceCodeInfo} />
        {value.type === 'message' && (<div>
          {value.descriptor.field.length > 0 && <div>
            <h2>Fields</h2>
            {value.descriptor.field.map(f => (<div key={f.name}>
              <h4><code>{formatFieldType(f)}&nbsp;{f.name}&nbsp;=&nbsp;{f.number};</code></h4>
              <Comment info={f.$sourceCodeInfo} />
            </div>))}
          </div>}
          {value.descriptor.extension.length > 0 && <div><h2>Options</h2>
            {value.descriptor.extension.map(e => (<div key={e.name}>
              <h4><code>{formatFieldType(e)}&nbsp;{e.name}&nbsp;=&nbsp;{e.number};</code></h4>
              <Comment info={e.$sourceCodeInfo} />
            </div>))}</div>}
        </div>)}
        {value.type === 'enum' && (<div>
          {value.descriptor.value.length > 0 && <div>
            <h2>Values</h2>
            {value.descriptor.value.map(v => (<div key={v.name}>
              <h4><code>{v.name}&nbsp;=&nbsp;{v.number};</code></h4>
              <Comment info={v.$sourceCodeInfo} />
            </div>))}
          </div>}
        </div>)}
        {value.type === 'service' && (<div>
          {value.descriptor.method.length > 0 && <div>
            <h2>Methods</h2>
            {value.descriptor.method.map(m => (<div key={m.name}>
              <h4><code>rpc&nbsp;{m.name}&nbsp;({
                streamingness(m.clientStreaming)
              }{formatLinkedType(m.inputType)
              })&nbsp;returns&nbsp;({
                streamingness(m.clientStreaming)
              }{
                formatLinkedType(m.outputType)
              });</code></h4>
              <Comment info={m.$sourceCodeInfo} />
            </div>))}
          </div>}
        </div>)}
        {nestedPackages.length > 0 &&
        <NestedEntityTable title="Packages" entities={nestedPackages} />}
        {nestedServices.length > 0 &&
        <NestedEntityTable title="Services" entities={nestedServices} />}
        {nestedMessages.length > 0 &&
        <NestedEntityTable title="Messages" entities={nestedMessages} />}
        {nestedEnums.length > 0 &&
        <NestedEntityTable title="Enums" entities={nestedEnums} />}
        {nestedOptions.length > 0 &&
        <NestedEntityTable title="Options" entities={nestedOptions} />}
      </main>
    </Card>;
  } else {
    return <div style={{textAlign: 'center', margin: '20px 0'}}><Spin size="large" /></div>;
  }
};

const mapStateToProps = (state: State): $Shape<Props> => ({
  value: state.descriptors.entity,
});

const mapDispatchToProps = (dispatch: Action => void): $Shape<Props> => ({});

export default connect(mapStateToProps, mapDispatchToProps)(Entity);
