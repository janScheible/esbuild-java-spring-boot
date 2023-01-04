import { render, h, Fragment } from '~/preact/dist/preact.module';

export default class WebComponent extends HTMLElement {
	constructor() {
		super();

		const animals = ['Dog', 'Bird', 'Cat', 'Mouse', 'Horse'];

		render(
			<>
				<style>
					{`
					:host {
						display: inline-block;
						background-color: #f1f4c6;
					}

					h1 {
						background-color: #d6d0b8;
					`}
				</style>

				<h1>Web Component :-)</h1>

				<button onClick={this.onButtonClick}>Click me!</button>

				<ul>
					{animals.map((animal) => (
						<li style={{ fontSize: '24px' }}>{animal}</li>
					))}
				</ul>
			</>,
			this.attachShadow({ mode: 'open' })
		);
	}

	onButtonClick(event: MouseEvent) {
		alert('hi ' + (event.target as HTMLElement).tagName + '!');
	}
}

customElements.define('es-web-component', WebComponent);
